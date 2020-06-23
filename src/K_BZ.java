import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.stream.IntStream;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.Label;

public class K_BZ {
	ArcLabelledImmutableGraph G;
	long E = 0;
	int n;
	int md; // max eta-degree

	double eta;
	int[] etadeg;
	BitSet gone;
	BitSet invalid;
	// invalid tells for each vertex v if the real eta degree is
	// the same as the value in etadeg or not.
	// If invalid.get(v) == false, then etadeg[v] is good.
	// Otherwise, eta degree of v needs to be recomputed.

	int[] vert;
	int[] pos;
	int[] deg; // array of lower bounds
	int[] bin;
	
	int precision;
	String DPtype;
	int L;
	
	int processors;

	public K_BZ(String basename, double eta, int L, int precision, String DPtype) throws Exception {
		G = ArcLabelledImmutableGraph.load(basename);
		this.eta = eta;
		this.DPtype = DPtype;
		this.L = L;

		n = G.numNodes();

		deg = new int[n];
		etadeg = new int[n];
		gone = new BitSet(n);
		invalid = new BitSet(n);
		this.precision = precision;
		this.eta = eta;
	
		

		md = 0;
		
		// Approximates initial eta-degrees using central limit theorem. It uses
		// "inverseCumulativeProbability" for computation
		long startTime = System.currentTimeMillis();
		
		processors = Runtime.getRuntime().availableProcessors();
		System.out.println("Number of processors is " + processors);
		
		IntStream.range(0, processors)
        .parallel()
        .forEach(processor -> {
        	computeInitialEtaDegs_inProcessor(processor);
		});
		
		//Previous version
		/*
		for(int v=0; v<n; v++) {
			
			deg[v] = KCoins.LYc(
					G.labelArray(v), 
					G.outdegree(v), 
					G.outdegree(v), 
					eta,precision);
			
		}
		*/
		
		System.out.println("Time elapsed for initial (sec) = " + (System.currentTimeMillis() - startTime) / 1000.0);
		
		for (int i = 0; i < n; i++) {
			invalid.set(i, true); // initially all the nodes on their
									// lower-bound

			if (deg[i] >= md) {
				md = deg[i];
			}
		}

		vert = new int[n];
		pos = new int[n];

		bin = new int[md + 1]; // md+1 because we can have zero degree
	}

	
	private void computeInitialEtaDegs_inProcessor(int processor) {
		//computing range of nodes for which to compute initial eta-deg
		int[] range = getRangeOfNodes(processor);
		
		ArcLabelledImmutableGraph H = G.copy();
		
		for (int v = range[0]; v < range[1]; v++) {
			
			int temp = KCoins.LYc(
					H.labelArray(v), 
					H.outdegree(v), 
					H.outdegree(v), 
					eta,precision) - (eta > 0 ? 1 : 0); 
					//We correct by -1 because, in very few cases, LYc overestimates and doesn't produce a lower bound.
					//We only do the correction if eta>0.
					//If eta=0, LYc is very accurate, so no need to correct.
			
			deg[v] = temp < 0 ? 0 : temp;
			
			/*
			deg[v] = KCoins.LYc(
					H.labelArray(v), 
					H.outdegree(v), 
					H.outdegree(v), 
					eta,precision); */
				
			}
	}

	//Return an array "range" of size 2. range[0] is "from", range[1] is "to". 
	private int[] getRangeOfNodes(int processor) {
		int num_nodes = n / processors;
		int[] range = new int[2];
		range[0] = processor * num_nodes;
		range[1] = processor != processors-1 ? (processor+1) * num_nodes : n;
		return range;
	}
	
	
	public int[] KCoreCompute() {

		for (int d = 0; d <= md; d++)
			bin[d] = 0;
		for (int v = 0; v < n; v++) {
			bin[deg[v]]++;
		}

		int start = 0; // start=1 in original, but no problem (same as
						// deterministic case)
		for (int d = 0; d <= md; d++) {
			int num = bin[d];
			bin[d] = start;
			start += num;
		}

		// bin-sort vertices by degree (same as deterministic case)
		for (int v = 0; v < n; v++) {
			pos[v] = bin[deg[v]];
			vert[pos[v]] = v;
			bin[deg[v]]++;
		}
		// recover bin[] (same as deterministic case)
		for (int d = md; d >= 1; d--)
			bin[d] = bin[d - 1];
		bin[0] = 0; // 1 in original (same as deterministic case)

		// main algorithm
		for (int i = 0; i < n;) {

			int v = vert[i]; // smallest degree vertex that will be deleted

			if (invalid.get(v) == false) {

				gone.set(v);

				int v_deg = G.outdegree(v);
				E += v_deg;
				int[] N_v = G.successorArray(v);
				for (int j = 0; j < v_deg; j++) {
					int u = N_v[j];

					if (deg[u] == deg[v] && invalid.get(u) == true) {

						recompute_and_swap(u);
						invalid.clear(u);
					}

					if (deg[u] > deg[v]) {
						swap_left(u);
						invalid.set(u);
					}
				}
				i++;
			} else {
				recompute_and_swap(v);
			}
		}

		return deg;
	}

	void recompute_and_swap(int v) {
		etadeg[v] = recompute(v);
		invalid.clear(v);
		int diff = etadeg[v] - deg[v];

		for (int d = 0; d < diff; d++)
			swap_right(v);
	}

	void swap_left(int u) {
		int du = deg[u];
		int pu = pos[u];
		int pw = bin[du];
		int w = vert[pw];
		if (u != w) {
			pos[u] = pw;
			vert[pu] = w;
			pos[w] = pu;
			vert[pw] = u;
		}
		bin[du]++;
		deg[u]--;

	}

	void swap_right(int u) {
		int du = deg[u];
		int pu = pos[u];
		int pw = bin[du + 1] - 1;
		int w = vert[pw];
		if (u != w) {
			pos[u] = pw;
			vert[pu] = w;
			pos[w] = pu;
			vert[pw] = u;
		}
		bin[du + 1]--;
		deg[u]++;
	}

	
	int recompute(int v) {
		int v_deg_real;

		// rebuild label_array for v
		v_deg_real = 0; // real degree, i.e. num of neighbors still alive
		int[] v_neighbors = G.successorArray(v);
		int v_deg = G.outdegree(v);
		for (int t = 0; t < v_deg; t++) {
			int u = v_neighbors[t];

			if (gone.get(u) == false)
				v_deg_real++;
		}

		int Index = 0;

		Label[] v_label_real = new Label[v_deg_real];
		Label[] v_label = G.labelArray(v);
		for (int t = 0; t < v_deg; t++) {
			int u = v_neighbors[t];

			if (gone.get(u) == false) {
				v_label_real[Index] = v_label[t];
				Index++;
			}
		}
		
		
		
        if(v_deg_real>L) {
            return KCoins.LYc(v_label_real, v_deg_real, v_deg_real, eta, precision );
        }
        else {
        	if (DPtype.equals("DP_log2"))
        		return KCoins.DP_log2(v_label_real, v_deg_real, v_deg_real, eta, precision );
        	else
        		return KCoins.DP(v_label_real, v_deg_real, v_deg_real, eta, precision );
        }
		
	}

	
	public void writeResults(int[] core, String filename) throws IOException {
		BufferedWriter w = new BufferedWriter(new FileWriter(filename));
		for (int v = 0; v < n; v++) {
			w.write(v + "\t" + core[v]);
			w.write("\n");
		}
		w.close();
	}
	
	
	public static void main(String[] args) throws Exception {

		//args = new String[] { "Flickr-proc.w", "0.0", "16", "DP_log2"};

		
		if (args.length < 4) {
			System.err.println("Specify: basename eta L precision DPtype(optional)");
			System.exit(1);
		}

		String basename = args[0];
		double eta = Double.parseDouble(args[1]);
		int L = Integer.parseInt(args[2]);
		int precision = Integer.parseInt(args[3]);
		String DPtype = args.length==5 ? args[4] : "DP"; 

		System.out.println("Starting " + basename);
		
		K_BZ tt = new K_BZ(basename, eta, L, precision, DPtype);

		long startTime = System.currentTimeMillis();
		int[] res = tt.KCoreCompute();
		System.out.println(args[0] + ": Time elapsed (sec) = " + (System.currentTimeMillis() - startTime) / 1000.0);
		
		int kmax = -1;
		double sum = 0;
		int cnt = 0;
		for (int i = 0; i < res.length; i++) {
			if (res[i] > kmax)
				kmax = res[i];
			sum += res[i];
			if (res[i] > 0)
				cnt++;
		}
		System.out.println("|V|	|E|	etadmax	kmax	kavg");
		System.out.println(cnt + "\t" + (tt.E / 2) + "\t" + tt.md + "\t" + kmax + "\t" + (sum / cnt));

		System.out.println();
		
		tt.writeResults(res, basename+"eta-" + eta + "-bz.txt");
	}
}
