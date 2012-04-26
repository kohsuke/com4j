COM4J uses native library, so an entire JVM can load it only once. This prevents us from building the whole sample in
one go, so we copy com4j and tlbimp once here, and pass it via "ant -lib".