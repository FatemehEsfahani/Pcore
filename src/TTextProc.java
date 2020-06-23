import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.SortedMap;
import java.util.TreeMap;

public class TTextProc {

	public static void main(String[] args) throws Exception {
		
		args = new String[] {
				"../webgraphs/francesco/Flickr.txt", 
				"Flickr-proc.txt"};
		
		if(args.length != 2) 
			System.out.println("Specify: inputfile outfile");

		SortedMap<Integer, SortedMap<Integer, Double>> G = new TreeMap<>();
		
		BufferedReader r = new BufferedReader(new FileReader(new File(args[0])));
		
		String line = null;
		while((line = r.readLine()) != null) {
            String[] arr = line.split("\\s+");
            int u = Integer.parseInt(arr[0]);
            int v = Integer.parseInt(arr[1]);
            double value = Double.parseDouble(arr[2]);
            
            if(u % 10000 == 0)
            	System.out.println(u);
            
            if(u == v) 
            	continue; //remove self-loops
            
            if(!G.containsKey(u)) 
            	G.put(u, new TreeMap<>());
            
            if(!G.containsKey(v)) 
            	G.put(v, new TreeMap<>());
            
            G.get(u).put(v, value);
            G.get(v).put(u, value);
            
        }
		
		r.close();
		
		
		BufferedWriter w = new BufferedWriter(new FileWriter(args[1]));
		for (Integer u : G.keySet())
			for (Integer v : G.get(u).keySet())
				w.write(u + "\t" + v + "\t" + String.format("%.16f", G.get(u).get(v)) + "\n");
		w.close();
		

	}

}
