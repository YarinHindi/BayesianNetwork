import java.text.DecimalFormat;
import java.util.*;

public class BayesianNetwork {

    public HashMap<String, BayesianNode> networkNodes;
    public static double numerator = 0;
    public static double denominator = 0;
    public static int numberOfAddOp = 0;
    public static int numberOfMulOp = 0;

    public BayesianNetwork() {
        this.networkNodes = new HashMap<>();
        this.numberOfAddOp = 0;
        this.numberOfMulOp = 0;
    }

    public void updateNetFromXml(String XmlFile) {
        XmlParser.fileToNetwork(XmlFile, this.networkNodes);
    }

    public BayesianNode returnNodebyName(String name) {
        return networkNodes.get(name);
    }

    public static void resetAns() {
        numerator = 0;
        denominator = 0;
        numberOfAddOp = 0;
        numberOfMulOp = 0;
    }

    public ArrayList<ArrayList<String>> returnEvidenceWithoutoutCome(ArrayList<String> evidence) {
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
        ArrayList<ArrayList<String>> evidenceWithoutComes = returnEvidenceWithoutoutCome(evidence);

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
                if (numberOfAddOp > 0) {
                    numberOfAddOp--;
//                    numberOfAddOp+=networkNodes.get(queryVar).outComes.size()-1;
//                    numberOfAddOp-= networkNodes.get(queryVar).outComes.size();

                }
                ans = (String.format("%.5f", numerator / (denominator + numerator))) + "," + numberOfAddOp + "," + numberOfMulOp;
                break;
            case "2":
                double preAns = variableElimination(evidenceWithoutComes, hidden, queryVar, query,outComequeryVar);
                ans = (String.format("%.5f", preAns)) + "," + numberOfAddOp + "," + numberOfMulOp;
                break;
            case "3":
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

    public double variableElimination(ArrayList<ArrayList<String>> evidence, ArrayList<String> hidden, String queryVar, String query,String queryOutcomeVar) {
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
        for (int i = 0; i < hidden.size(); i++) {
            int numberOfFactorContain = NumFactorsContain(factors, hidden.get(i));
            int index = factorIndexForHidden(factors,hidden.get(i));
            while (numberOfFactorContain>1){
                int [] smallest = findIndexOfSmallestFactors(factors,hidden.get(i));
                HashMap<String, Double> factor1 = new HashMap<>();
                factor1.putAll(factors.get(smallest[0]));
                HashMap<String, Double> factor2 = new HashMap<>();
                factor2.putAll(factors.get(smallest[1]));
                if (smallest[0] < smallest[1]) {
                    factors.remove(smallest[1]);
                    factors.remove(smallest[0]);
                }
                else {
                    factors.remove(smallest[0]);
                    factors.remove(smallest[1]);
                }
                factors.add(join(factor1, factor2));
                index = factors.size()-1;
                numberOfFactorContain = NumFactorsContain(factors, hidden.get(i));
            }
            HashMap<String, Double> cpt = new HashMap<>();
            cpt.putAll(eliminate(factors.get(index), hidden.get(i)));
            factors.set(index, cpt);
            deleteEmptyFactor(factors);
        }
        // checking if we have multiple cpt's that contains the query variable
        int index = factorIndexForHidden(factors, queryVar);
        int numberOfFactorContain = NumFactorsContain(factors, queryVar);
        // while numbers of the cpt's that contains the query
        // is greater than 1, then we will keep doing join
        while (numberOfFactorContain > 1) {
            int [] check = findIndexOfSmallestFactors(factors, queryVar);
            HashMap<String, Double> factor1 = new HashMap<>();
            factor1.putAll(factors.get(check[0]));
            HashMap<String, Double> factor2 = new HashMap<>();
            factor2.putAll(factors.get(check[1]));
            if (check[0] < check[1]) {
                factors.remove(check[1]);
                factors.remove(check[0]);
            }
            else {
                factors.remove(check[0]);
                factors.remove(check[1]);
            }
            factors.add(join(factor1, factor2));
            index = factors.size()-1;
            numberOfFactorContain = factorIndexForHidden(factors, queryVar);
        }
        HashMap<String, Double> final_factor = normalize(factors.get(index));
        //  keys of this factor
        ArrayList<String> keys = new ArrayList<>(final_factor.keySet());
        // getting the query with it's outcome
        String queryVarWithOutCome = queryVar+"="+queryOutcomeVar;
        // checking in the factor the specific case that we want
        double final_prob = check_prob(queryVarWithOutCome, keys, final_factor);
        return final_prob;

    }
    public HashMap<String, Double> eliminate(HashMap<String, Double> factor, String hidden){
        ArrayList<String> keys = new ArrayList<>(factor.keySet());
        HashMap<String, Double> newFactor = new HashMap<>();
        double value = 0;
        Boolean inside = false;
        for (int i = 0; i < keys.size(); i++) {
            String keyWithHidden = keys.get(i);
            String keyWithOutHidden = removeHidden(keyWithHidden,hidden);
            value = factor.get(keys.get(i));
            inside = false;
            if (keyWithOutHidden.length()==3) return newFactor;
            for (int j = i+1; j < keys.size(); j++) {
                String keyWithHiddenCheck = keys.get(j);
                String keyWithOutHiddenCheck = removeHidden(keyWithHiddenCheck,hidden);
                if(keyWithOutHidden.equals(keyWithOutHiddenCheck)){
                    value+=factor.get(keys.get(j));
                    numberOfAddOp++;
                    inside = true;
                }
            }
            if (inside) newFactor.put(keyWithOutHidden,value);
        }
        return newFactor;

    }
    public double check_prob(String query, ArrayList<String> keys, HashMap<String, Double> factor) {
        double prob = 0;
        for (String key : keys) {
            if (key.contains(query)) {
                prob = factor.get(key);
                break;
            }
        }

        return prob;
    }
    public String removeHidden(String key , String hidden) {
        String ans = "P(";
        ArrayList<String> newKey = nodeNamesFromRowOfCpt(key);
        boolean first = true;
        for (int i = 0; i < newKey.size(); i++) {
            if (newKey.get(i).contains(hidden)) continue;
            if (first) {
                first = false;
                int ind = newKey.get(i).indexOf("=");
                ans += newKey.get(i).substring(0, ind) +"="+ newKey.get(i).substring(ind + 1, newKey.get(i).length());
            } else {
                int ind = newKey.get(i).indexOf("=");
                ans += "," + newKey.get(i).substring(0, ind) +"="+ newKey.get(i).substring(ind + 1, newKey.get(i).length());
            }

        }
        ans+=")";
        return ans;
    }

    public int[] findIndexOfSmallestFactors(ArrayList<HashMap<String,Double>> factors,String hidden) {
        int[] ans = new int[2];
        HashMap<String, Double> min_cpt_1 = new HashMap<>();
        int min = Integer.MAX_VALUE;
        int min_index1 = 0;
        int firstSize = 0;
        int secondSize = 0;
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            if (key.contains(hidden)) {
                if (factors.get(i).size() < min) {
                    min = factors.get(i).size();
                    min_index1 = i;
                    firstSize = min;
                }
            }
        }
        min_cpt_1.putAll(factors.get(min_index1));
        HashMap<String, Double> min_cpt_2 = new HashMap<>();
        min = Integer.MAX_VALUE;
        int min_index2 = 100;
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            // checking the second min size
            if (key.contains(hidden)) {
                if (factors.get(i).size() < min&& i!=min_index1) {
                    min = factors.get(i).size();
                    min_index2 = i;
                    secondSize = min;
                }
            }
        }
        min_cpt_2.putAll(factors.get(min_index2));
        if (min_cpt_1.size() < min_cpt_2.size()) {
            ans[0] = min_index1;
            ArrayList<HashMap<String, Double>> temps = new ArrayList<>();
            for (int i = 0; i < factors.size(); i++) {
                ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
                ArrayList<String> names = nodeNamesFromRowOfCpt(check.get(0));
                // checking if there's more factors with the second min size
                for (int j = 0; j < names.size(); j++) {
                    if (names.get(j).substring(0,names.get(j).indexOf("=")).equals(hidden)) {
                        if (i != ans[0]) {
                            if (factors.get(i).size() == min) {
                                temps.add(factors.get(i));
                            }
                        }
                    }
                }
            }
            // if there's only one factor with the second min size we can add it'sindex for sure
            if (temps.size() == 1) {
                ans[1] = min_index2;
            }
            else {
                // if there's more than one factor with this size,
                // we will check the it's min value by ascii
                HashMap<String, Double> min_hash = ascii_check(temps);
                for (int i = 0; i < factors.size(); i++) {
                    if (min_hash.equals(factors.get(i))) {
                        ans[1] = i;
                    }
                }
            }
        }
        else {
            // if first min size equals to second min size
            ArrayList<HashMap<String, Double>> temps = new ArrayList<>();
            for (int i = 0; i < factors.size(); i++) {
                ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
                ArrayList<String> names = nodeNamesFromRowOfCpt(check.get(0));
                // checking if there's more factors with their min size
                for (int j = 0; j < names.size(); j++) {
                    if (names.get(j).substring(0,names.get(j).indexOf("=")).equals(hidden)) {
                        if (factors.get(i).size() == min) {
                            temps.add(factors.get(i));
                        }
                    }
                }
            }
            // if we have only 2 factors with this size (first min and second min)
            // their indexes will be out answer
            if (temps.size() == 2) {
                ans[0] = min_index1;
                ans[1] = min_index2;
            }
            else {
                // if there's more than one factor with this size,
                // we will check the it's min value by ascii
                HashMap<String, Double> min_hash = ascii_check(temps);
                for (int i = 0; i < factors.size(); i++) {
                    if (min_hash.equals(factors.get(i))) {
                        ans[0] = i;
                    }
                }
                ArrayList<HashMap<String, Double>> temps1 = new ArrayList<>();
                for (int i = 0; i < factors.size(); i++) {
                    ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
                    ArrayList<String> names = nodeNamesFromRowOfCpt(check.get(0));
                    // check if there are more factors with the same size
                    for (int j = 0; j < names.size(); j++) {
                        if (names.get(j).substring(0,names.get(j).indexOf("=")).equals(hidden)) {
                            if (i != ans[0]) {
                                if (factors.get(i).size() == min) {
                                    temps1.add(factors.get(i));
                                }
                            }
                        }
                    }
                }
                // returns the index of the minimun by ascii
                HashMap<String, Double> min_hash1 = ascii_check(temps1);
                for (int i = 0; i < factors.size(); i++) {
                    if (min_hash1.equals(factors.get(i))) {
                        ans[1] = i;
                    }
                }
            }
        }
        return ans;
    }



    public HashMap<String, Double> ascii_check(ArrayList<HashMap<String, Double>> temps) {
        int min = Integer.MAX_VALUE;
        int index = 0;
        for (int i = 0; i < temps.size(); i++) {
            ArrayList<String> check = new ArrayList<>(temps.get(i).keySet());
            String key = check.get(0);
            // getting the names of the nodes that are in the key
            ArrayList<String> variables = nodeNamesFromRowOfCpt(key);
            int sum = 0;
            // computing their ascii sum
            for (int j = 0; j < variables.size(); j++) {
                int ind = variables.get(j).indexOf("=");
                String var = variables.get(j).substring(0,ind);
                if (var.length() == 1) {
                    sum += variables.get(j).charAt(0);
                }
                else {
                    for (int k = 0; k < var.length(); k++) {
                        sum += variables.get(j).charAt(k);
                    }
                }
            }
            if (sum < min) {
                min = sum;
                index = i;
            }
        }
        return temps.get(index);
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
                        if(k< firstOf1.size()-1) {
                            newKey += firstOf1.get(k) + ",";
                        }else{
                            newKey += firstOf1.get(k);
                        }
                    }
                    for (int k = 0; k < firstOf2.size(); k++) {
                        if (k==0)newKey+=",";
                        String var = firstOf2.get(k).substring(0, firstOf2.get(k).indexOf("="));
                        if (commonVar.contains(var)) continue;
                        if (k <firstOf2.size() - 1) {
                            newKey += firstOf2.get(k)+",";
                        } else {
                            newKey += firstOf2.get(k) ;
                        }
                    }
                    if (newKey.charAt(newKey.length()-1)==','){
                        newKey = newKey.substring(0,newKey.length()-1);
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

    // return all vars in query with their outcome in this format --> [[A=T], [B=F]].
    public ArrayList<String> nodeNamesFromRowOfCpt(String cptRow) {
        if (cptRow.contains("|")) {
            int ind = cptRow.indexOf("|");
            String firstVar = cptRow.substring(2, ind);
            if (cptRow.length() > ind + 2) {
                int start = cptRow.indexOf("|") + 1;
                int end = cptRow.indexOf(")");
                String subbed = cptRow.substring(start, end);
                if (subbed.contains(",")) {
                    ArrayList<String> names = new ArrayList<>(Arrays.asList(subbed.split(",")));
                    names.add(0, firstVar);
                    return names;
                } else {
                    ArrayList<String> names = new ArrayList<>();
                    names.add(firstVar);
                    names.add(subbed);
                    return names;
                }
            } else {
                ArrayList<String> names = new ArrayList<>();
                names.add(firstVar);
                return names;
            }
        } else {
            ArrayList<String> names = new ArrayList<>();
            String[] toPut = cptRow.substring(2, cptRow.indexOf(")")).split(",");
            names.addAll(Arrays.asList(toPut));
            return names;
        }
    }

//    public ArrayList<String> nodeWithoutComes(String valueOfRow){
//        ArrayList<String> varNames = new ArrayList<>();
//        if(valueOfRow.contains("|")){
//
//        }
//
//    }

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
    public int factorIndexForHidden(ArrayList<HashMap<String, Double>> factors, String node) {
        for (int i = 0; i < factors.size(); i++) {
            ArrayList<String> check = new ArrayList<>(factors.get(i).keySet());
            String key = check.get(0);
            if (key.contains(node)) return i;
            }
        // if no factor containing that node
        return -1;
    }
    public HashMap<String, Double> join2(HashMap<String, Double> factor1, HashMap<String, Double> factor2) {
        HashMap<String, Double> multiply = new HashMap<>();
        ArrayList<String> keys1 = new ArrayList<>(factor1.keySet());
        ArrayList<String> keys2 = new ArrayList<>(factor2.keySet());
        // going through each key of factor1
        for (int i = 0; i < keys1.size(); i++) {
            String key_check1 = keys1.get(i);
            String key1 = "";
            if (key_check1.charAt(0) == 'P') {
                int ind = key_check1.indexOf("|");
                if (ind + 2 == key_check1.length()) {
                    key1 = key_check1.substring(2,ind);
                }
                else {
                    key1 = key_check1.substring(2,ind)+","+key_check1.substring(ind+1, key_check1.length()-1);
                }
            }
            else {
                key1 = key_check1;
            }
            String [] key1_vars = key1.split(",");
            // going through each key of factor2
            for (int j = 0; j < keys2.size(); j++) {
                boolean should_multiply = true;
                String key_check2 = keys2.get(j);
                String key2 = "";
                if (key_check2.charAt(0) == 'P') {
                    int ind = key_check2.indexOf("|");
                    if (ind + 2 == key_check2.length()) {
                        key2 = key_check2.substring(2,ind);
                    }
                    else {
                        key2 = key_check2.substring(2,ind)+","+key_check2.substring(ind+1, key_check2.length()-1);
                    }
                }
                else {
                    key2 = key_check2;
                }
                String [] key2_vars = key2.split(",");
                ArrayList<String> diffrent = new ArrayList<>();
                // going through every variable in each key of factor 2
                for (int k = 0; k < key2_vars.length; k++) {
                    int i2 = key2_vars[k].indexOf("=");
                    String var2 = key2_vars[k].substring(0,i2);
                    boolean is_diff = true;
                    // going through every variable in each key of factor 1
                    for (int l = 0; l < key1_vars.length; l++) {
                        int i1 = key1_vars[l].indexOf("=");
                        String var1 = key1_vars[l].substring(0,i1);
                        // checking if they are different
                        if (var1.equals(var2)) {
                            is_diff = false;
                            String out1 = key1_vars[l].substring(i1+1);
                            String out2 = key2_vars[k].substring(i2+1);
                            // it they are not different
                            // checking if their outcome is different
                            if (!out1.equals(out2)) {
                                should_multiply = false;
                                l = key1_vars.length-1;
                                k = key2_vars.length-1;
                            }
                        }
                    }
                    // after we went through all of key 1
                    //  we can know for sure if it's variable is different
                    if (is_diff) {
                        diffrent.add(key2_vars[k]);
                    }
                }
                // if every same variable has the same outcome
                // we would like to multiply their rows
                if (should_multiply) {
                    // adding the num of mul operations
                    // setting the correct key for this multiplication
                    String key = key1;
                    for (int k = 0; k < diffrent.size(); k++) {
                        key += ","+diffrent.get(k);
                    }
                    double multiple = factor1.get(keys1.get(i)) * factor2.get(keys2.get(j));
                    // adding to our joined factor the key and the probability
                    multiply.put(key, multiple);
                }
            }
        }
        return multiply;
    }
    public HashMap<String, Double> normalize(HashMap<String, Double> factor) {
        ArrayList<String> keys = new ArrayList<>(factor.keySet());
        double sum = 0;
        double prob = 0;
        for (int i = 0; i < keys.size(); i++) {
            // adding the number of adding operations
            numberOfAddOp++;
            // calculating the su, of all of the probabilities
            sum += factor.get(keys.get(i));
        }
        // example: if we have 4 variables we want to sum, then we'll have 3 add operations
        numberOfAddOp--;
        // removing and adding the updated (normalized) probability
        for (int i = 0; i < keys.size(); i++) {
            double prb = factor.get(keys.get(i));
            factor.remove(keys.get(i));
            factor.put(keys.get(i), prb/sum);
        }
        return factor;
    }
}




