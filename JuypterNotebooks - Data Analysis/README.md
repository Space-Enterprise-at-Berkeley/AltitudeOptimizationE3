# DispAnalysisModel-SEB

## Running the Notebook:

1. Run `pip install -r requirements.txt`. This will install all necessary python libraries to run the notebooks for analysis.
2. Configure your python environment. You may use a `Conda` environment or select a preset kernel. I personally used the 3.11.4 global env kernel.

## Troubleshooting:

1. A common issue is that the imports will not immediately work directly after installing all required libraries. This is easily fixed by restarting your kernel (or IDE in the worst case) or setting up a `Conda` environment and running `conda install --file requirements.txt -c conda-forge` to install dependencies. 

## To Do List:
- Confidence interval (start with 95 and 99.5) of rocket landing distance to the launch site. Ideally this includes direction, cuz a lot of the rockets land in the bottom right corner, and not the top left, so having that in consideration