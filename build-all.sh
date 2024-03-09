./gradlew publishToMavenLocal
mvn install:install-file -Dfile=example/ps-118/JHyphenator-1.0.jar -DgroupId=mfietz \
  -DartifactId=jhyphenator -Dversion=1.0 -Dpackaging=jar

rm -f example/ps-118/output/*
kotlin example/ps-118/convert-to-pdf.main.kts && \
  lo-kts-converter/lo-kts-converter.main.kts \
    -i example/ps-118/output/ps-118.fodt -f pdf && \
  lo-kts-converter/lo-kts-converter.main.kts \
    -i example/ps-118/output/ps-118-ebook.fodt -f pdf

rm -f example/builder/output/* && \
  kotlin example/builder/table.main.kts && \
  lo-kts-converter/lo-kts-converter.main.kts \
    -i example/builder/output/letter.fodt -f pdf,odt,docx

rm -f example/mvp-doc/output/* && \
  kotlin example/mvp-doc/mvp-doc.main.kts && \
  lo-kts-converter/lo-kts-converter.main.kts \
    -i example/mvp-doc/output/mvp-doc.fodt -f pdf

rougify style github > example/mvp-doc/syntax.css