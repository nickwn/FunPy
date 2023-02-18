# RTV
RTV is a projection model that projects the details of local, automatic variables, inputs, and outputs in a boxed modal.

## How to develop RTV
All of the rtv front-end-related code is under `./src/vs/editor/contrib/rtv/browser`.

There are few requirements before you could develop for RTV

1. Node JS 16.14+
2. Python 3 preferably 3.8.* or above
3. npm
4. yarn (this could be installed using npm)

Once you satisfy the requierments please follow the next steps to setup your environment

1. type `yarn install` in the vscode directory
2. Then open the vscode dircetory using your own VS Code local installation (you could download it online if you don't have one installed)
3. In the `launch.json` under `.vscode` directory set the following env variables under `Launch VS Code Internal`
```
PYTHON3: path to your python3 binary
RUNPY: absolute path to the run.py under src directory
SYNTH: [optional, for SnipPy only] absolute path to the jar file of scala synthesizer
SCALA: [optional, for SnipPy only] path to your scala interpreter
```
- You may also configure these environment variables in your shell configuration (e.g., in `~/.profile`).
- Alternatively, you may configure the variables in `./.env`, and declare in `inputs` in `.vscode/launch.json` the id's and keys of the configured variables. For example, if you have configured `PYTHON3=/usr/local/bin/python3` in `./.env`, then the `inputs` in `./vscode/launch.json` should be
```
	"inputs": [
		{
			"id": "envPYTHON3",
			"type": "command",
			"command": "extension.commandvariable.file.content",
			"args": {
			  "fileName": "${workspaceFolder}/.env",
			  "key": "PYTHON3",
			  "default": ""
			}
		}
	]
```
and the environment variable configuration in `Launch VS Code Internal`, under `.vscode/launch.json`, should be
```
"env": {
	"PYTHON3": "${input:envPYTHON3}"
}
```

4. Then press `CTRL + SHIFT + B` or `CMD + SHIFT + B` on mac and run with `Launch VS Code Internal` to build the configuration

If everything goes well you should be able run the build by pressing `F5`. Open a python file with extension .py, and you should see the projection boxes.

## (Experimental) Installing Copilot
We are still exploring ways to install the GitHub Copilot to Projection Boxes so that you can have fun with both!

For now, please use the following instructions for installing GitHub Copilot
(tested on Windows and macOS 12.5.1 with VSCode installed, Copilot version 1.62.7527):
1. Install Copilot on your VSCode (not the build with Copilot).
2. When done, copy the entire directory with a name like `github.copilot-<version_number>` under `~/.vscode/extensions` to `~/.vscode-oss-dev/extensions`. Note that the directory `~/.vscode-oss-dev` may not be available until after you build and run the VSCode with Projections Boxes.
3. Build and run the VSCode with Projection Boxes as usual. Copilot should show up now!


## How the synthesizer gets called
The synthesizer is called within the `synthesizeFragment` function in the `RTVDisplay.ts` file
