package net.idik.lib.slimadapter

/**
 * Type-safe, reflection-free registration extension for Kotlin 2.x+.
 *
 * Replaces SlimAdapter's legacy 2-arg Java `register(layoutRes, SlimInjector<T>)` which relied on
 * `getClass().getGenericInterfaces()` — broken under Kotlin's default `invokedynamic` SAM lambdas.
 */
inline fun <reified T : Any> SlimAdapter.register(
    layoutRes: Int,
    slimInjector: SlimInjector<T>
): SlimAdapter = register(T::class.javaObjectType, layoutRes, slimInjector)
