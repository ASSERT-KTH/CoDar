# CoDar
Fixes SonarQube warnings using ChatGPT!

## Handled Rules
Checkout the prompt templates [here](https://github.com/khaes-kth/CoNar/tree/main/files/prompts). Add more in new PRs :)

## Example
```
gh repo clone khaes-kth/CoDar
gh repo clone didi/sds
cd sds
git checkout 46dac33f39bc1720ca108bd61b6375874906ad57
cd ..
wget https://github.com/SpoonLabs/sorald/releases/download/sorald-0.8.0/sorald-0.8.0-jar-with-dependencies.jar
java -jar sorald-0.8.0-jar-with-dependencies.jar mine --source sds/ --handled-rules --stats-output-file mine-sds.json
cd CoDar
mvn package
export CODEX_API_TOKEN={{YOUR-OPENAI-API-TOKEN}}
java -jar target/CoDar-1.0-SNAPSHOT-jar-with-dependencies.jar --root ../sds --mine-res ../mine-sds.json --rule S2142 --prompt-type TITLE_DESCRIPTION_EXAMPLE
cd ../sds
git diff
```
