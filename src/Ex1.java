import java.util.*;

public class Ex1 {

    public static int ans = 0;

    public static void main(String[] args) {
//        Map<String,String> aa= new LinkedHashMap<>();
//        aa.put("ss","aa");
//        aa.put("aa","d");
//        final Set<Map.Entry<String, String>> mapValues = aa.entrySet();
//        String ss ="abv|d)";
//        System.out.println(ss.substring(ss.indexOf('|'),ss.indexOf(')')));
//
//
//        ArrayList<String > a = new ArrayList<>();
//        a.add("A=T");
//        a.add("B=F");
//        a.add("C=T");
//        System.out.println(a.subList(0,2));
//        ArrayList<String > sim = new ArrayList<>();
//        sim.add("D");
//        sim.add("E");
//        ArrayList<String > val = new ArrayList<>();
//        val.add("v1");
//        val.add("v2");
//        val.add("v3");
//        ArrayList<Boolean> list = new ArrayList<>(Arrays.asList(new Boolean[2]));
//        Collections.fill(list,Boolean.FALSE);
//        print(a,sim,0,val,list);
//        System.out.println((ans));


//    public static void print (ArrayList<String> str, ArrayList<String> symbol,int index,ArrayList<String> val,ArrayList<Boolean> taken){
//
//        if(index==symbol.size()){
//            System.out.println(str+" "+ans);
//            ans++;
//            return;
//        }
//        for(int i =index ;i<symbol.size();i++){
//            for (int j = 0; j < 3; j++) {
//                if (taken.get(i)) continue;
//                str.add(symbol.get(i)+"="+val.get(j));
//                taken.set(i,true);
//                print(str,symbol,index+1,val,taken);
//                str.remove(str.size()-1);
//                taken.set(i,false);
//            }
//        }
//    }
////        ArrayList<ArrayList<String>> a = new ArrayList<>();
////        ArrayList<String> b = new ArrayList<>();
////        ArrayList<String> c = new ArrayList<>();
////        ArrayList<String> d = new ArrayList<>();
////        ArrayList<String> e = new ArrayList<>();
////        b.add("B");
////        b.add("T");
////        c.add("B0");
////        c.add("v1");
////        d.add("C");
////        d.add("T");
////        e.add("A");
////        e.add("T");
////        a.add(b);
////        a.add(c);
////        a.add(d);
////        a.add(e);
////        System.out.println(a);
////        Collections.sort(a,Comparator.comparing((ArrayList<String> p)->p.get(0)));
////        System.out.println(a);
//
        BayesianNetwork net = new BayesianNetwork();
        BayesianNetwork net2 = new BayesianNetwork();
        net.updateNetFromXml("alarm_net.xml");

        System.out.println(net.networkNodes.size());
        for (Map.Entry<String, BayesianNode> set : net.networkNodes.entrySet()) {
            System.out.println(set.getValue().name + " : " + set.getValue().outComes + " cpt = " + set.getValue().cpt +
                    " Children : " + set.getValue().children + " Parents :" + set.getValue().parents);
        }
    }
}
//
//}
