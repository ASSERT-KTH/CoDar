# Conar
Fixes SonarQube warnings using Codex!

## Handled Rules
Checkout the prompt templates [here](https://github.com/khaes-kth/CoNar/tree/main/files/prompts). Add more in new PRs :)

## Example
```
gh repo clone khaes-kth/CoDar
gh repo clone didi/sds
cd sds
git checkout 46dac33f39bc1720ca108bd61b6375874906ad57
cd ..
wget https://github.com/SpoonLabs/sorald/releases/download/sorald-0.8.0/sorald-0.8.0-jar-with-dependencies.jar sorald.jar
java -jar sorald.jar mine --source sds/ --handled-rules --stats-output-file mine-sds.json
cd CoNar
mvn package
export CODEX_API_TOKEN={{YOUR-OPENAI-API-TOKEN}}
# java -jar target/sonar-codex-1.0-SNAPSHOT-jar-with-dependencies.jar {input-file-path} {output-file-path} {replacement-startline} {replacement-endline} {non-compliant-line} {rule}
java -jar target/sonar-codex-1.0-SNAPSHOT-jar-with-dependencies.jar ../sds/sds-admin/src/main/java/com/didiglobal/sds/admin/util/BloomFileter.java ../BloomFileter.java 28 28 28 2184
java -jar target/sonar-codex-1.0-SNAPSHOT-jar-with-dependencies.jar --root ../sds --mine-res ../mine-sds.json --rule S2142 --prompt-type TITLE_DESCRIPTION_EXAMPLE
git diff --no-index ../sds/sds-admin/src/main/java/com/didiglobal/sds/admin/util/BloomFileter.java ../BloomFileter.java
```
