package org.apache.gora.util;

public class ClassLoadingUtils {

    private ClassLoadingUtils() {
        //Utility Class
    }

    /**
     * Loads a class using the class loader.
     * 1. The class loader of the current class is being used.
     * 2. The thread context class loader is being used.
     * If both approaches fail, throws ClassNotFoundException.
     *
     * @param className The name of the class to load.
     * @return The class.
     * @throws ClassNotFoundException If and only if no definition for the class with the specified name could be found.
     */
    public static Class<?> loadClass(String className) throws ClassNotFoundException {
        return ClassLoadingUtils.loadClass(ClassLoadingUtils.class,className);
    }

    /**
     * Loads a class using the class loader.
     * 1. The class loader of the context class is being used.
     * 2. The thread context class loader is being used.
     * If both approaches fail, throws ClassNotFoundException.
     *
     * @param contextClass The name of a context class to use.
     * @param className    The name of the class to load.
     * @return The class.
     * @throws ClassNotFoundException If and only if no definition for the class with the specified name could be found.
     */
    public static Class<?> loadClass(Class<?> contextClass, String className) throws ClassNotFoundException {
        Class<?> clazz = null;
        if (contextClass.getClassLoader() != null) {
            clazz = loadClass(className, contextClass.getClassLoader());
        }
        if (clazz == null && Thread.currentThread().getContextClassLoader() != null) {
            clazz = loadClass(className, Thread.currentThread().getContextClassLoader());
        }
        if (clazz == null) {
            throw new ClassNotFoundException("Failed to load class " + className);
        }
        return clazz;
    }

    /**
     * Loads a {@link Class} from the specified {@link ClassLoader} without throwing {@ClassNotFoundException}.
     *
     * @param className The name of the class to load.
     * @param classLoader Class loader instance where given class to be loaded.
     * @return The class or null if no class loader could load the class.
     */
    private static Class<?> loadClass(String className, ClassLoader classLoader) {
        Class<?> clazz = null;
        if (classLoader != null && className != null) {
            try {
                clazz = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                //Ignore and return null
            }
        }
        return clazz;
    }
}
