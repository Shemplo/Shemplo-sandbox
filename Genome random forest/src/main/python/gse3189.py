import numpy  as np
import pandas as pd
import sys

from sklearn.feature_selection import chi2, f_classif, mutual_info_classif
from sklearn.ensemble  import RandomForestClassifier, ExtraTreesClassifier
from sklearn.feature_selection import SelectKBest, SelectFromModel, RFECV
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.metrics import accuracy_score, make_scorer
from sklearn.svm import SVC, LinearSVC

train_input = pd.read_csv ("../../../results/train.csv", index_col = 0)

feature_names = train_input.columns.values
train_data    = train_input.values [:, :7504]
train_target  = train_input.values [:, -1]

np.random.seed (163 + int(sys.argv [1]))
train_data, test_data, train_target, test_target =\
	train_test_split (train_data, train_target, test_size = 0.7)

estimators = 60 # run parameter
forest = RandomForestClassifier (n_estimators=estimators,
                                 random_state=0)
forest.fit (train_data, train_target)

scorer = make_scorer (accuracy_score)
scores = scorer (forest, test_data, test_target)
print "Match score: %f" % scores 

importances = forest.feature_importances_
std = np.std ([tree.feature_importances_ for tree in forest.estimators_],
              axis=0)
indices = np.argsort (importances) [::-1]							  

output = open ("sklearn-%s.txt" % str (estimators), 'w')
for f in range(train_data.shape[1]):
	output.write ("%s %f" % (feature_names [indices [f]], importances [indices [f]]))
	output.write ("\n")
output.close ()

