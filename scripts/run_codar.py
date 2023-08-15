import sys
import os
import multiprocessing as mp
from os.path import exists
import time


def repair(repo, promptType, rule):
    currentPath = os.getcwd()
    projName = repo.split('/')[-1].split('.git')[0]
    os.system(f'git clone {repo} repo')
    os.system(f'java -jar sorald.jar mine --source repo/ --stats-output-file {projName}-before.json')
    os.system(f'java -jar codar.jar --root {currentPath}/repo --mine-res {currentPath}/{projName}-before.json --rule {rule} --prompt-type {promptType}')
    os.system(f'java -jar sorald.jar mine --source repo/ --stats-output-file {projName}-after-{rule}-{promptType}.json')
    os.system(f'rm -rf repo')

def process(repo, promptConfigsFile):
    with open(promptConfigsFile) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        for promptConfig in lines:
            repair(repo, promptConfig.split(',')[0], promptConfig.split(',')[1])

def main(argv):
    promptConfigsFile = argv[1]
    with open(argv[0]) as file:
        lines = file.readlines()
        lines = [line.rstrip() for line in lines]
        for repo in lines:
            process(repo, promptConfigsFile)

if __name__ == "__main__":
    main(sys.argv[1:])