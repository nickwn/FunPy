CURR_DIR=$PWD;

if [[ ! -f ~/LooPy/synthesizer/target/snippy-server-0.1-SNAPSHOT-jar-with-dependencies.jar ]]; then
    echo "[WARN] Synthesizer jar does not existing. Building...";
    cd ~/LooPy/synthesizer;
    mvn install -Plocal -DskipTests &> build.log;
    if [[ ! $! -eq 0 ]]; then
       echo "[ERROR] Synthesizer build failed:";
       cat build.log;
       cd $CURR_DIR;
       exit 1;
    fi;
    cd $CURR_DIR;
fi;

if [[ -d vscode ]]; then
    cd vscode;
else
    if [[ -d ../vscode ]]; then
        cd ../vscode;
    fi;
fi;

SNIPPY_UTILS=~/LooPy/vscode/src/snippy.py RUNPY=~/LooPy/vscode/src/run.py IMGSUM=~/LooPy/vscode/src/img-summary.py PYTHON3=$(which python3) JAVA=$(which java) SYNTH='../synthesizer/target/snippy-server-0.1-SNAPSHOT-jar-with-dependencies.jar' ./scripts/code.sh;

cd $CURR_DIR;
