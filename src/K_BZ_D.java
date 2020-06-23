import java.io.File;
import java.io.PrintStream;

import it.unimi.dsi.webgraph.ImmutableGraph;

public class K_BZ_D {
	ImmutableGraph G;
	boolean printprogress = false; 
	long E=0;
	int n; 
	int md; //max degree
	
	public K_BZ_D(String basename) throws Exception {
		G = ImmutableGraph.load(basename);
		
		n = G.numNodes();
		
		md = 0;
		for(int v=0; v<n; v++) {
			int v_deg = G.outdegree(v);
			if(md < v_deg) 
				md = v_deg;
		}
	}
	
    public int[] KCoreCompute () {

    	int[] vert = new int[n];
    	int[] pos = new int[n];
    	int[] deg = new int[n];
    	int[] bin = new int[md+1]; //md+1 because we can have zero degree 

    	for(int d=0; d<=md; d++) 
    		bin[d] = 0;
    	for(int v=0; v<n; v++) { 
    		deg[v] = G.outdegree(v);
    		bin[ deg[v] ]++;
    	}

    	int start = 0; //start=1 in original, but no problem
    	for(int d=0; d<=md; d++) {
    		int num = bin[d];
    		bin[d] = start;
    		start += num;
    	}
    	
    	//bin-sort vertices by degree
    	for(int v=0; v<n; v++) {
    		pos[v] = bin[ deg[v] ];
    		vert[ pos[v] ] = v;
    		bin[ deg[v] ]++;
    	}
    	//recover bin[]
    	for(int d=md; d>=1; d--) 
    		bin[d] = bin[d-1];
    	bin[0] = 0; //1 in original
    	
    	//main algorithm
    	long pctDoneLastPrinted = 0;
    	for(int i=0; i<n; i++) {
    		
    		int v = vert[i]; //smallest degree vertex
    		int v_deg = G.outdegree(v);
    		int[] N_v = G.successorArray(v);
    		for(int j=0; j<v_deg; j++) {
    			int u = N_v[j];

    			if(deg[u] > deg[v]) {
    				int du = deg[u]; int pu = pos[u];
    				int pw = bin[du]; int w = vert[pw];
    				if(u!=w) {
    					pos[u] = pw; vert[pu] = w;
    					pos[w] = pu; vert[pw] = u;
    				}
    				bin[du]++;
    				deg[u]--;
    			}
    		}
    		
    		
    		long pctDone = Math.round( (100.0*(i+1))/n ); 
    		if ( pctDone >= pctDoneLastPrinted + 10 || pctDone == 100) { 
    			System.out.println("pctDone=" + pctDone + "%");
    			pctDoneLastPrinted = pctDone;
    		}
    	}
    	
    	return deg;
    }
    
	public static void main(String[] args) throws Exception {
		long startTime = System.currentTimeMillis();
		
		args = new String[] {"Flickr-proc"};
		
		if(args.length != 1) {
			System.err.println("Usage: java K_BZ_D basename");
			System.exit(1);
		}
		
		String basename = args[0];
		
		System.out.println("Starting " + basename);
		K_BZ_D kc = new K_BZ_D(basename);
		
		//storing the core value for each node in a file.
		PrintStream ps = new PrintStream(new File("bzCoreD.txt"));
		
		int[] res = kc.KCoreCompute();
		int kmax = -1;
		double sum = 0;
		int cnt = 0;
		for(int i=0; i<res.length; i++) {
			ps.println(i+"\t" + res[i]);
			if(res[i] > kmax) 
				kmax = res[i];
			sum += res[i];
			if(res[i] > 0) cnt++;
		}
		System.out.println("|V|	|E|	dmax	kmax	kavg");
		System.out.println(cnt + "\t" + (kc.E/2) + "\t" + kc.md + "\t" + kmax + "\t" + (sum/cnt) );
		
        System.out.println(args[0] + ": Time elapsed (sec) = " + (System.currentTimeMillis() - startTime)/1000.0);
	}
}