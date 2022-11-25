
import java.util.*;

public class BayesianNetwork {

    public HashMap<String, BayesianNode> networkNodes;
    public double numerator = 0;
    public double denominator = 0;
    public int numberOfAddOp = 0;
    public int numberOfMulOp = 0;

    public BayesianNetwork() {
        this.networkNodes = new HashMap<>();
    }

    public void updateNetFromXml(String XmlFile) {
        XmlParser.fileToNetwork(XmlFile, this.networkNodes);
    }


    public void resetAns() {
        numerator = 0;
        denominator = 0;
        numberOfAddOp = 0;
        numberOfMulOp = 0;
    }

    public ArrayList<ArrayList<String>> returnEvidenceWithoutCome(ArrayList<String> evidence) {
        ArrayList<ArrayList<String>> evidencewithoutCome = new ArrayList<>();
        ArrayList<String> temp = new ArrayList<>();
        for (int i = 0; i < evidence.size(); i++) {
            int ind = evidence.get(i).indexOf("=");
            temp.add(evidence.get(i).substring(0, ind));
            temp.add(evidence.get(i).substring(ind + 1, evidence.get(i).length()));
            evidencewithoutCome.add(new ArrayList<>(temp));
            temp.clear();
        }
        return evidencewithoutCome;
    }

    /// should check it
    public double checkIfAnsInCpt(String query, ArrayList<String> evidence, String queryVar, String outComeQueryVar) {
        Boolean con1;
        Boolean con2;
        for (Map.Entry<String, Double> set : networkNodes.get(queryVar).cpt.entrySet()) {
            con1 = true;
            con2 = true;
            if (evidence.size() > 1) {
                if (set.getKey().contains(queryVar + "=" + outComeQueryVar + "|")) {
                    for (int i = 1; i < evidence.size(); i++) {
                        if (!set.getKey().contains(evidence.get(i))) con1 = false;
                    }
                    ArrayList<String> queryNodeNames = nodeNamesFromRowOfCpt(set.getKey());
                    for (int i = 0; i < queryNodeNames.size(); i++) {
                        if (!query.contains(queryNodeNames.get(i))) con2 = false;
                    }
                    if (con1 && con2) return set.getValue();
                }
            } else {
                if (set.getKey().contains(queryVar + "=" + outComeQueryVar) && !set.getKey().contains("|"))
                    return set.getValue();
            }
        }
        return -1;
    }

    //hidden in a format of only vars name,
    //evidence are in format name = outcome example --> [B=T , J=T]
    //queryVar is only the name of var
    //output var is the output
    //query is the question that we ask for
    public String AnsweringQuery(ArrayList<String> evidence, ArrayList<String> hidden, String queryVar, String outComequeryVar, String whichCodeToRun, String query) {
        resetAns();
        String ans = "";
        double beforeAns;
        //list of list with evidence with their outcomes example ==> [[A,T] [B,F] [C,v1]]
        ArrayList<ArrayList<String>> evidenceWithoutComes = returnEvidenceWithoutCome(evidence);
        beforeAns = checkIfAnsInCpt(query, evidence, queryVar, outComequeryVar);
        if (beforeAns != -1) {
            return (String.format("%.5f", beforeAns)) + "," + numberOfAddOp + "," + numberOfMulOp;
        }
        switch (whichCodeToRun) {
            case "1":
                ArrayList<Boolean> taken = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
                Collections.fill(taken, Boolean.FALSE);
                AllCombinationToCompute(evidence, hidden, taken, 0, queryVar, outComequeryVar, false);
                hidden.add(queryVar);
                evidence.remove(queryVar + "=" + outComequeryVar);
                ArrayList<Boolean> taken2 = new ArrayList<>(Arrays.asList(new Boolean[hidden.size()]));
                Collections.fill(taken2, Boolean.FALSE);
                AllCombinationToCompute(evidence, hidden, taken2, 0, queryVar, outComequeryVar, true);
                numberOfAddOp--;
                ans = (String.format("%.5f", numerator / (denominator + numerator))) + "," + numberOfAddOp + "," + numberOfMulOp;
                break;
            case "2":
                double preAns = variableElimination(evidenceWithoutComes, hidden, queryVar, query, outComequeryVar, true);
                ans = (String.format("%.5f", preAns)) + "," + numberOfAddOp + "," + numberOfMulOp;
                break;
            case "3":
                double preAns2 = variableElimination(evidenceWithoutComes, hidden, queryVar, query, outComequeryVar, false);
                ans = (String.format("%.5f", preAns2)) + "," + numberOfAddOp + "," + numberOfMulOp;
                break;
        }
        return ans;
    }

    public void AllCombinationToCompute(ArrayList<String> str, ArrayList<String> hidden, ArrayList<Boolean> taken, int index, String queryVar, String outComequeryVar, Boolean flag) {
        if (index == hidden.size()) {
            if (!flag) {
                numerator += answerForOneRow(str);
            } else {
                denominator += answerForOneRow(str);
            }
            numberOfAddOp++;
            return;
        }
        for (int i = index; i < hidden.size(); i++) {
            for (int j = 0; j < networkNodes.get(hidden.get(i)).outComes.size(); j++) {
                if (!flag) {
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1, queryVar, outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                } else {
                    if (hidden.get(i).equals(queryVar) && networkNodes.get(hidden.get(i)).outComes.get(j).equals(outComequeryVar))
                        continue;
                    if (taken.get(i)) continue;
                    str.add(hidden.get(i) + "=" + networkNodes.get(hidden.get(i)).outComes.get(j));
                    taken.set(i, true);
                    AllCombinationToCompute(str, hidden, taken, index + 1, queryVar, outComequeryVar, flag);
                    str.remove(str.size() - 1);
                    taken.set(i, false);
                }
            }
        }
    }


    public ArrayList<HashMap<String, Double>> getAllFactorsContainHidden(ArrayList<HashMap<String, Double>> factors, String hidden) {
        ArrayList<HashMap<String, Double>> factContainHidden = new ArrayList<>();
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            if (key.contains(hidden)) {
                factContainHidden.add(new HashMap<>(factors.get(i)));
            }
        }
        return factContainHidden;
    }

    public int HeuristicsOrder(ArrayList<String> hidden, ArrayList<HashMap<String, Double>> factors, ArrayList<String> alreadyEliminated) {
        int ans = -1;
        int min = Integer.MAX_VALUE;
        int currntableSize;
        ArrayList<String> varsInTable = new ArrayList<>();
        for (int i = 0; i < hidden.size(); i++) {
            if (alreadyEliminated.contains(hidden.get(i))) continue;
            ;
            ArrayList<HashMap<String, Double>> hiddenInvolve = getAllFactorsContainHidden(factors, hidden.get(i));
            if (hiddenInvolve.size() == 1) {
                if (hiddenInvolve.get(0).keySet().size() < min) {
                    min = hiddenInvolve.get(0).keySet().size();
                    ans = i;
                }
                break;
            }
            currntableSize = 0;
            varsInTable.clear();
            for (int j = 0; j < hiddenInvolve.size() - 1; j++) {
                if (j == 0) {
                    currntableSize = hiddenInvolve.get(0).size();
                    ArrayList<String> temp = new ArrayList<>(hiddenInvolve.get(j).keySet());
                    varsInTable = getNodesNameWithOutOutCome(temp.get(j));
                }
                ArrayList<String> before = new ArrayList<>(hiddenInvolve.get(j + 1).keySet());
                ArrayList<String> CommonVars = getCommonVarsbykey(varsInTable, before.get(0));
                int counter = 1;
                for (int k = 0; k < CommonVars.size(); k++) {
                    counter *= networkNodes.get(CommonVars.get(k)).outComes.size();
                }
                int sizeOfCommonVars = CommonVars.size();
                int sizeSecondTable = hiddenInvolve.get(j + 1).size();
                currntableSize = currntableSize * sizeSecondTable / counter;
            }
            if (currntableSize < min) {
                min = currntableSize;
                ans = i;
            }

        }
        return ans;

    }

    public double answerForOneRow(ArrayList<String> rowToCompute) {
        HashMap<String, String> helper = new HashMap<>();
        double ans = 0;
        boolean first = true;
        String keyToCompute;
        numberOfMulOp += rowToCompute.size() - 1;
        for (int i = 0; i < rowToCompute.size(); i++) {
            int indexForVar = rowToCompute.get(i).indexOf('=');
            String var = rowToCompute.get(i).substring(0, indexForVar);
            String varOutCome = rowToCompute.get(i).substring(indexForVar + 1);
            helper.put(var, varOutCome);
        }
        for (Map.Entry<String, String> set :
                helper.entrySet()) {
            keyToCompute = "P(";
            keyToCompute += set.getKey() + "=" + set.getValue();
            if (networkNodes.get(set.getKey()).parents.size() > 0) keyToCompute += '|';
            for (int i = 0; i < networkNodes.get(set.getKey()).parents.size(); i++) {
                String currParent = networkNodes.get(set.getKey()).parents.get(i);
                keyToCompute += currParent + "=" + helper.get(currParent);
                if (i < networkNodes.get(set.getKey()).parents.size() - 1) {
                    keyToCompute += ',';
                }
            }
            keyToCompute += ')';
            if (first) {
                ans = networkNodes.get(set.getKey()).cpt.get(keyToCompute);
                first = false;
            } else {
                ans *= networkNodes.get(set.getKey()).cpt.get(keyToCompute);
            }
        }
        return ans;
    }

    public void getAncestors(BayesianNode var, HashSet<String> set) {
        if (var == null) {
            return;
        }
        for (int i = 0; i < var.parents.size(); i++) {
            set.add(var.parents.get(i));
            getAncestors(networkNodes.get(var.parents.get(i)), set);
        }
    }

    public double variableElimination(ArrayList<ArrayList<String>> evidence, ArrayList<String> hidden, String queryVar, String query, String queryOutcomeVar, Boolean flag) {
        //drop all node that  not ancestor of evidence and query var.
        HashSet<String> relevantNode = dropAllVarsNotAncestor(evidence, queryVar);
        //init the factors
        ArrayList<HashMap<String, Double>> factors = initFactors();
        //delete the rows that include the evidence we don't need.
        deleteEvidenceFromFactors(factors, evidence);
        //delete factor taht are size 0
        deleteEmptyFactor(factors);
        // gives us the names of nodes in query with their outcomes
        ArrayList<String> namesWithOutComes = nodeNamesFromRowOfCpt(query);
        for (int i = hidden.size() - 1; i >= 0; i--) {
            if (!relevantNode.contains(hidden.get(i))) {
                //remove all cpt that contain nodes are not ancestors of evidence and queryVar.
                for (int j = factors.size() - 1; j >= 0; j--) {
                    ArrayList<String> vals = new ArrayList<>(factors.get(j).keySet());
                    String firstItem = vals.get(0);
                    if (firstItem.contains(hidden.get(i))) {
                        factors.remove(j);
                    }
                }
                hidden.remove(i);
            }
        }
        if (flag) Collections.sort(hidden);
        ArrayList<String> eliminated = new ArrayList<>();
        int index;
        for (int i = 0; i < hidden.size(); i++) {
            if (!flag) {
                i = HeuristicsOrder(hidden, factors, eliminated);
            }
            index = joinAll(factors, hidden.get(i));
            HashMap<String, Double> cpt = new HashMap<>();
            cpt.putAll(eliminate(factors.get(index), hidden.get(i)));
            if (!flag) {
                eliminated.add(hidden.get(i));
                if (eliminated.size() != hidden.size() && i == hidden.size() - 1) i--;
                if (eliminated.size() == hidden.size()) i = hidden.size();
            }
            factors.set(index, cpt);
            deleteEmptyFactor(factors);
        }
        index = joinAll(factors, queryVar);
        HashMap<String, Double> final_factor = normalize(factors.get(index));
        ArrayList<String> keys = new ArrayList<>(final_factor.keySet());
        String queryVarWithOutCome = queryVar + "=" + queryOutcomeVar;
        double final_prob = findAnswerAfterNormalize(queryVarWithOutCome, keys, final_factor);
        return final_prob;
    }

    public int joinAll(ArrayList<HashMap<String, Double>> factors, String var) {
        int numberOfFactorContain = NumFactorsContain(factors, var);
        int index = factorIndexForHidden(factors, var);
        while (numberOfFactorContain > 1) {
            int[] smallest = findIndexOfSmallestFactors(factors, var);
            int ind1 = smallest[0];
            int ind2 = smallest[1];
            HashMap<String, Double> firstFactor = new HashMap<>();
            firstFactor.putAll(factors.get(ind1));
            HashMap<String, Double> secondFactor = new HashMap<>();
            secondFactor.putAll(factors.get(ind2));
            if (ind1 < ind2) {
                int temp = ind1;
                ind1 = ind2;
                ind2 = temp;
            }
            factors.remove(ind1);
            factors.remove(ind2);
            factors.add(join(firstFactor, secondFactor));
            index = factors.size() - 1;
            numberOfFactorContain = NumFactorsContain(factors, var);
        }
        return index;

    }

    public HashMap<String, Double> eliminate(HashMap<String, Double> factor, String hidden) {
        ArrayList<String> keys = new ArrayList<>(factor.keySet());
        HashMap<String, Double> newFactor = new HashMap<>();
        double value = 0;
        Boolean inside = false;
        for (int i = 0; i < keys.size(); i++) {
            String keyWithHidden = keys.get(i);
            String keyWithOutHidden = removeHidden(keyWithHidden, hidden);
            value = factor.get(keys.get(i));
            inside = false;
            if (keyWithOutHidden.length() == 3) return newFactor;
            for (int j = i + 1; j < keys.size(); j++) {
                String keyWithHiddenCheck = keys.get(j);
                String keyWithOutHiddenCheck = removeHidden(keyWithHiddenCheck, hidden);
                if (keyWithOutHidden.equals(keyWithOutHiddenCheck)) {
                    value += factor.get(keys.get(j));
                    numberOfAddOp++;
                    inside = true;
                }
            }
            if (inside) newFactor.put(keyWithOutHidden, value);
        }
        return newFactor;

    }

    public double findAnswerAfterNormalize(String queryVarWithOutCome, ArrayList<String> keys, HashMap<String, Double> factor) {
        double finalAnswer = 0;
        for (String key : keys) {
            if (key.contains(queryVarWithOutCome)) {
                finalAnswer = factor.get(key);
                break;
            }
        }
        return finalAnswer;
    }

    public String removeHidden(String key, String hidden) {
        String ans = "P(";
        ArrayList<String> newKey = nodeNamesFromRowOfCpt(key);
        boolean first = true;
        for (int i = 0; i < newKey.size(); i++) {
            if (newKey.get(i).contains(hidden)) continue;
            if (first) {
                first = false;
                int ind = newKey.get(i).indexOf("=");
                ans += newKey.get(i).substring(0, ind) + "=" + newKey.get(i).substring(ind + 1, newKey.get(i).length());
            } else {
                int ind = newKey.get(i).indexOf("=");
                ans += "," + newKey.get(i).substring(0, ind) + "=" + newKey.get(i).substring(ind + 1, newKey.get(i).length());
            }
        }
        ans += ")";
        return ans;
    }



    public int findOneSmalletFactor(ArrayList<HashMap<String, Double>> factors, String hidden,int taken){
        int min = Integer.MAX_VALUE;
        int min_index = -1;
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            if (key.contains(hidden)) {
                if (factors.get(i).size() < min && i!=taken) {
                    min = factors.get(i).size();
                    min_index = i;
                }
            }
        }
        return min_index;
    }


    public int[] findIndexOfSmallestFactors(ArrayList<HashMap<String, Double>> factors, String hidden) {
        int[] ans = new int[2];
        HashMap<String, Double> min_cpt_1 = new HashMap<>();
        int min_index1 = findOneSmalletFactor(factors,hidden,-1);

        HashMap<String, Double> min_cpt_2 = new HashMap<>();
        int min_index2 = findOneSmalletFactor(factors,hidden,min_index1);

        int firstMinSize = factors.get(min_index1).size();
        int secondMinSize = factors.get(min_index2).size();
        if(firstMinSize<secondMinSize){
            ans[0] = min_index1;
            ans[1] = minAscii(factors,secondMinSize,hidden,-1);
        }else{
            ans[0] =  minAscii(factors,firstMinSize,hidden,-1);
            ans[1] = minAscii(factors,firstMinSize,hidden,ans[0]);
        }
    return ans;
    }

    public int minAscii(ArrayList<HashMap<String, Double>> factors,int size,String hidden,int taken) {
        int min = Integer.MAX_VALUE;
        int ans = 0;
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            ArrayList<String> variables = nodeNamesFromRowOfCpt(key);
            int sum = 0;
            if(!(key.contains(hidden))||check.size()!=size || i==taken)continue;
            for (int j = 0; j < variables.size(); j++) {
                int ind = variables.get(j).indexOf("=");
                String var = variables.get(j).substring(0, ind);
                for (int k = 0; k < var.length(); k++) {
                    sum += variables.get(j).charAt(k);
                }
            }
            if (sum < min) {
                min = sum;
                ans = i;
            }
        }
        return ans;
    }

    public ArrayList<String> getCommonVars(HashMap<String, Double> first, HashMap<String, Double> second) {
        ArrayList<String> commonVar = new ArrayList<>();
        ArrayList<String> preCommonVarsFirst = new ArrayList<>(first.keySet());
        ArrayList<String> commonVarsFirst = nodeNamesFromRowOfCpt(preCommonVarsFirst.get(0));
        ArrayList<String> preCommonVarsSecond = new ArrayList<>(second.keySet());
        String commonVarsSecond = (preCommonVarsSecond.get(0));
        for (int i = 0; i < commonVarsFirst.size(); i++) {
            String currCommon = commonVarsFirst.get(i).substring(0, commonVarsFirst.get(i).indexOf("="));
            if (commonVarsSecond.contains(currCommon)) {
                commonVar.add(currCommon);
            }
        }
        return commonVar;
    }

    public ArrayList<String> getCommonVarsbykey(ArrayList<String> firstKey, String secondKey) {
        ArrayList<String> second = getNodesNameWithOutOutCome(secondKey);
        ArrayList<String> ans = new ArrayList<>();
        for (int i = 0; i < firstKey.size(); i++) {
            if (second.contains(firstKey.get(i))) ans.add(firstKey.get(i));
        }
        return ans;
    }

    public HashMap<String, Double> join(HashMap<String, Double> first, HashMap<String, Double> second) {
        ArrayList<String> commonVar = getCommonVars(first, second);
        HashMap<String, Double> newFactor = new HashMap<>();
        ArrayList<String> firstFactorKeys = new ArrayList<>(first.keySet());
        ArrayList<String> secondFactorKeys = new ArrayList<>(second.keySet());
        String newKey = "";
        Boolean ok = true;
        for (int i = 0; i < firstFactorKeys.size(); i++) {
            double Val1 = first.get(firstFactorKeys.get(i));
            String key1 = firstFactorKeys.get(i);
            ArrayList<String> firstOf1 = nodeNamesFromRowOfCpt(key1);
            for (int j = 0; j < secondFactorKeys.size(); j++) {
                double Val2 = second.get(secondFactorKeys.get(j));
                String key2 = secondFactorKeys.get(j);
                ArrayList<String> firstOf2 = nodeNamesFromRowOfCpt(key2);
                ok = true;
                newKey = "";
                for (int k = 0; k < firstOf1.size(); k++) {
                    String var = firstOf1.get(k).substring(0, firstOf1.get(k).indexOf("="));
                    if (!key2.contains(firstOf1.get(k)) && commonVar.contains(var)) {
                        ok = false;
                        break;
                    }
                }
                if (ok) {
                    numberOfMulOp++;
                    newKey = "P(";
                    for (int k = 0; k < firstOf1.size(); k++) {
                        if (k < firstOf1.size() - 1) {
                            newKey += firstOf1.get(k) + ",";
                        } else {
                            newKey += firstOf1.get(k);
                        }
                    }
                    for (int k = 0; k < firstOf2.size(); k++) {
                        if (k == 0) newKey += ",";
                        String var = firstOf2.get(k).substring(0, firstOf2.get(k).indexOf("="));
                        if (commonVar.contains(var)) continue;
                        if (k < firstOf2.size() - 1) {
                            newKey += firstOf2.get(k) + ",";
                        } else {
                            newKey += firstOf2.get(k);
                        }
                    }
                    if (newKey.charAt(newKey.length() - 1) == ',') {
                        newKey = newKey.substring(0, newKey.length() - 1);
                    }

                    newKey += ")";
                    newFactor.put(newKey, Val1 * Val2);
                }

            }
        }
        return newFactor;
    }

    public int NumFactorsContain(ArrayList<HashMap<String, Double>> factors, String node) {
        int counter = 0;
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String keyRowVal = check.get(0);
            if (keyRowVal.contains(node)) counter++;
        }
        return counter;
    }

    public HashSet<String> dropAllVarsNotAncestor(ArrayList<ArrayList<String>> evidence, String queryVar) {
        HashSet<String> ancestorSet = new HashSet<>();
        getAncestors(networkNodes.get(queryVar), ancestorSet);
        for (int i = 0; i < evidence.size(); i++) {
            getAncestors(networkNodes.get(evidence.get(i).get(0)), ancestorSet);
        }
        return ancestorSet;
    }

    public ArrayList<HashMap<String, Double>> initFactors() {
        ArrayList<HashMap<String, Double>> factors = new ArrayList<>();
        ArrayList<String> NodeNames = new ArrayList<>(networkNodes.keySet());
        for (int i = 0; i < NodeNames.size(); i++) {
            factors.add(new HashMap<>(networkNodes.get(NodeNames.get(i)).cpt));
        }
        return factors;
    }

    public ArrayList<String> getNodesNameWithOutOutCome(String cptRow) {
        ArrayList<String> beforeAns = nodeNamesFromRowOfCpt(cptRow);
        ArrayList<String> ans = new ArrayList<>();
        for (int i = 0; i < beforeAns.size(); i++) {
            ans.add(beforeAns.get(i).substring(0, beforeAns.get(i).indexOf("=")));
        }
        return ans;
    }

    // return all vars in query with their outcome in this format --> [[A=T], [B=F]].
    public ArrayList<String> nodeNamesFromRowOfCpt(String cptRow) {
        ArrayList<String> ans = new ArrayList<>();
        if(cptRow.contains("|")){
            int index1 = cptRow.indexOf("=");
            int index2 = cptRow.indexOf("|");
            ans.add(cptRow.substring(2,index1)+"="+cptRow.substring(index1+1,index2));
            if(cptRow.charAt(index2+1)!=')'){
                String [] splitted = cptRow.substring(index2+1,cptRow.length()-1).split(",");
                Collections.addAll(ans, splitted);
            }
        }else {
            String[] splitted = cptRow.substring(2, cptRow.indexOf(")")).split(",");
            Collections.addAll(ans, splitted);
        }
        return ans;
    }

    public void deleteEmptyFactor(ArrayList<HashMap<String, Double>> factors) {
        for (int i = factors.size() - 1; i >= 0; i--) {
            if (factors.get(i).size() == 0) factors.remove(factors.get(i));
        }
    }

    public void deleteEvidenceFromFactors(ArrayList<HashMap<String, Double>> factors, ArrayList<ArrayList<String>> evidence) {
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> currFactKeys = new ArrayList<>(factors.get(i).keySet());
            for (int j = 0; j < currFactKeys.size(); j++) {
                String currFactKey = currFactKeys.get(j);
                for (int k = 1; k < evidence.size(); k++) {
                    if (currFactKey.contains(evidence.get(k).get(0))) {
                        if (!currFactKey.contains(evidence.get(k).get(0) + "=" + evidence.get(k).get(1))) {
                            factors.get(i).remove(currFactKey);
                            break;
                        }
                    }
                }
            }
        }

    }

    public int factorIndexForHidden(ArrayList<HashMap<String, Double>> factors, String hidden) {
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> keys = new ArrayList<>(factors.get(i).keySet());
            String firstKey = keys.get(0);
            if (firstKey.contains(hidden)) return i;
        }
        return -1;
    }

    public HashMap<String, Double> normalize(HashMap<String, Double> lastFactor) {
        ArrayList<String> keys = new ArrayList<>(lastFactor.keySet());
        double sumToNormalize = 0;
        double beforeNormalizeProb;
        for (String key : keys) {
            numberOfAddOp++;
            sumToNormalize += lastFactor.get(key);
        }
        numberOfAddOp--;
        for (String key : keys) {
            beforeNormalizeProb = lastFactor.get(key);
            lastFactor.remove(key);
            lastFactor.put(key, beforeNormalizeProb / sumToNormalize);
        }
        return lastFactor;
    }
}




