package cookiedragon.eventsystem

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.ArrayList

/**
 * @author cookiedragon234 15/Feb/2020
 */
internal object EventDispatcherImpl: EventDispatcher {
	private val lookup = MethodHandles.lookup()
	private val subscriptions: MutableMap<Class<*>, MutableSet<SubscribingMethod<*>>> = ConcurrentHashMap()

	private val invokeQueue = PriorityQueue<SubscribingMethod<*>>(compareByDescending { it.priority })

	override fun <T : Any> dispatch(event: T): T {
		var clazz: Class<*> = event.javaClass
		val classes = mutableSetOf(clazz)
		while(clazz != Any::class.java) {
			clazz = clazz.superclass
			classes.add(clazz)
		}
		classes.forEach {
			subscriptions[it]?.let { methods ->
				for (method in methods) {
					if(method.active) invokeQueue.add(method)
				}
			}
		}

		while(invokeQueue.isNotEmpty()) {
			val method = invokeQueue.remove()
			method.invoke(event)
		}

		return event
	}
	
	override fun register(subscriber: Class<*>) = register(subscriber, null)
	
	override fun register(subscriber: Any) = register(subscriber.javaClass, subscriber)
	
	private fun register(clazz: Class<*>, instance: Any?) {
		for (method in clazz.declaredMethods) {

			// Needs Subscriber annotation
			if (!method.isAnnotationPresent(Subscriber::class.java))
				continue

			// If we are registering a static class then only allow static methods to be indexed
			if (instance == null && !method.isStatic())
				continue
			
			// If we are registering an initialised class then skip static methods
			if (instance != null && method.isStatic())
				continue

			val annotation = method.getAnnotation(Subscriber::class.java)
			
			if (method.returnType != Void.TYPE) {
				IllegalArgumentException("Subscriber $clazz.${method.name} cannot return type")
					.printStackTrace()
				continue
			}
			
			if (method.parameterCount != 1) {
				IllegalArgumentException("Expected only 1 parameter for $clazz.${method.name}")
					.printStackTrace()
				continue
			}
			method.isAccessible = true

			val eventType = method.parameterTypes[0]!!
			val methodHandle = lookup.unreflect(method)
//				.asType(MethodType.methodType(Void.TYPE, eventType))

			subscriptions.getOrPut(
				eventType, {
					hashSetOf()
				}
			).add(SubscribingMethod(clazz, instance, method.isStatic(), method, methodHandle, annotation.priority))
			subscriptions[eventType] = subscriptions[eventType]?.sortedWith(compareByDescending { it.priority })?.toMutableSet()!!
		}
	}
	
	
	override fun subscribe(subscriber: Class<*>) = setActive(subscriber, true)
	
	override fun subscribe(subscriber: Any)  = setActive(subscriber, true)
	
	override fun unsubscribe(subscriber: Class<*>)  = setActive(subscriber, false)
	
	override fun unsubscribe(subscriber: Any)  = setActive(subscriber, false)
	
	private fun setActive(instance: Any?, active: Boolean) {
		for (methods in subscriptions.values) {
			for (method in methods) {
				if (method.instance == instance) {
					method.active = active
				}
			}
		}
	}
	
	private fun setActive(subscriber: Class<*>, active: Boolean) {
		for (methods in subscriptions.values) {
			for (method in methods) {
				if (method.clazz == subscriber) {
					method.active = active
				}
			}
		}
	}
}


data class SubscribingMethod<T:Any>(val clazz: Class<*>, val instance: T?, val static: Boolean, val method: Method, val handle: MethodHandle, val priority: Int, var active: Boolean = false) {
	@Throws(Throwable::class)
	fun <E:Any> invoke(event: E) {
		if(static){
			handle.invoke(event)
		} else {
			handle.invoke(instance,event)
		}

	}
}

private fun Method.isStatic() = Modifier.isStatic(this.modifiers)
