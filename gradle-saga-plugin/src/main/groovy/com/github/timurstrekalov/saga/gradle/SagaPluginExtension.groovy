package com.github.timurstrekalov.saga.gradle

class SagaPluginExtension {

    File baseDir

    String includes

    String excludes

    File outputDir

    Boolean outputInstrumentedFiles

    List<String> noInstrumentPatterns

    Boolean cacheInstrumentedCode

    String outputStrategy

    Integer threadCount

    Boolean includeInlineScripts

    Long backgroundJavaScriptTimeout

    String sourcesToPreload

    String sourcesToPreloadEncoding

    String browserVersion

    String reportFormats

    String sortBy

    String order

}
