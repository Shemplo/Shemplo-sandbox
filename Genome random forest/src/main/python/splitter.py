import numpy  as np
import pandas as pd
import sys

from sklearn.feature_selection import chi2, f_classif, mutual_info_classif
from sklearn.ensemble  import RandomForestClassifier, ExtraTreesClassifier
from sklearn.feature_selection import SelectKBest, SelectFromModel, RFECV
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.metrics import accuracy_score, make_scorer
from sklearn.svm import SVC, LinearSVC

seed_shift = int   (sys.argv [1])
estimators = int   (sys.argv [2])
test_size  = float (sys.argv [3])

train_input = pd.read_csv ("../../../temp/dataset.csv", index_col = 0, sep = ";")

feature_names = train_input.columns.values
features_size = len (train_input.columns)

train_data    = train_input.values [:, :features_size - 1]
train_target  = train_input.values [:, -1]

train_data = pd.DataFrame (train_data)
train_target = pd.Series (train_target)

np.random.seed (163 + seed_shift)
train_data, test_data, train_target, test_target =\
	train_test_split (train_data, train_target, 
					  test_size = test_size)

# 17 -> 18 in tarin.csv
# 18 -> 19 in train.csv
"""
print train_data
for i in train_target.index:
	print "%d" % (i)
"""
output = open ("../../../temp/trainset.csv", 'w')
output.write ("geo_access\n")
for i in train_target.index:
	output.write ("%s\n" % train_input.iloc [i].name)
output.close ()