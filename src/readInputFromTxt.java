import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class readInputFromTxt {

    /**
     *
     * @param fileName the Xml file to load our network
     * @return The answer of the query we asked for in every line.
     */
    public static String readInputAndReturnAns(String fileName)  {
        String ans = "";
        try {
            File f = new File(fileName);
            Scanner sc = new Scanner(f);
            Set<String> set = new HashSet<>();
            BayesianNetwork network = new BayesianNetwork();
            network.updateNetFromXml(sc.nextLine());
            ArrayList<String> evidence = new ArrayList<>();
            ArrayList<String> hidden = new ArrayList<>();
            int indexOfQueryVar;
            int indexOfEndQueryVar;
            int indexEndOfQuery;
            String outComequeryVar;
            String withCodeToRun;
            String queryVar;
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String query = line.substring(0, line.length() - 2);
                indexOfQueryVar = line.indexOf('=');
                indexOfEndQueryVar = line.indexOf('|');
                indexEndOfQuery = line.indexOf(')');
                queryVar = line.substring(2, indexOfQueryVar);
                outComequeryVar = line.substring(indexOfQueryVar+1,indexOfEndQueryVar);
                evidence.add(queryVar+"="+outComequeryVar);
                set.add(queryVar);
                if(indexOfEndQueryVar+1!=indexEndOfQuery){
                ArrayList<String> restOfQuery = new ArrayList<>(Arrays.asList(line.substring(indexOfEndQueryVar + 1, indexEndOfQuery).split(",")));
                int indexVar;
                int endIndex;
                   for (int i = 0; i < restOfQuery.size(); i++) {
                       indexVar = restOfQuery.get(i).indexOf('=');
                       endIndex = restOfQuery.get(i).length();
                       evidence.add(restOfQuery.get(i).substring(0, indexVar) +
                               restOfQuery.get(i).substring(indexVar, endIndex));
                       set.add(restOfQuery.get(i).substring(0, indexVar));
                   }
               }

                for (Map.Entry<String, BayesianNode> set1 :
                        network.networkNodes.entrySet()) {
                    if(!set.contains(set1.getKey())){
                        hidden.add(set1.getKey());
                    }
                }
                withCodeToRun = line.substring(line.length()-1);
                ans +=  network.AnsweringQuery(evidence,hidden,queryVar,outComequeryVar,withCodeToRun,query)+"\n";
                hidden.clear();
                evidence.clear();
                set.clear();
            }
            return ans;

        }catch (FileNotFoundException e){
            System.out.println("Cannt read file!");
            return ans;
        }
    }

}
