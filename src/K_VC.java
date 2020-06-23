import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.IntStream;

import com.google.common.math.DoubleMath;

import it.unimi.dsi.webgraph.labelling.ArcLabelledImmutableGraph;
import it.unimi.dsi.webgraph.labelling.Label;

public class K_VC {
	ArcLabelledImmutableGraph G;
	int L;
	int num_of_edges_alive;
	int precision;

	int[] core; // stores core values (upper-bounds) at each iteration of the
				// algorithm.
	boolean[] scheduled; // the same as deterministic case
	int n;
	boolean printprogress = false;
	int iteration = 0;
	boolean change = false;
	static double[] prob_values;

	int[] etadeg; // stores initial eta-degrees
	double eta;
	String DPtype;
	
	int core_value;

	boolean etadegree_changedFlag;

	boolean verificationFlage; // when verificationFlage == true, it means that
								// verification step has been started
	int numOfverificationSteps = 0;
	boolean[] verificationRequired; // for each vertex, determines whether
									// verification is required or not.
	
	int processors;

	public K_VC(String basename, double eta, int L, int precision, String DPtype) throws Exception {
		G = ArcLabelledImmutableGraph.loadMapped(basename);
		this.eta = eta;
		this.L = L;
		this.precision = precision;
		this.DPtype = DPtype;
		
		core_value = 0;
		num_of_edges_alive = 0;

		n = G.numNodes();
		etadeg = new int[n];
		core = new int[n];
		scheduled = new boolean[n];

		verificationFlage = false;
		verificationRequired = new boolean[n];

		for (int v = 0; v < n; v++) // the same as deterministic case
			scheduled[v] = true;

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
		for (int v = 0; v < n; v++) {

			int v_deg = G.outdegree(v);

			if (v_deg == 0) {
				etadeg[v] = 0;
			} else {
				Label[] v_labels = G.labelArray(v);

				if (v_deg < L) {
					etadeg[v] = KCoins.DP_log2(v_labels, v_deg, v_deg, eta, precision);
				} else {
					etadeg[v] = KCoins.LYc(v_labels, v_deg, v_deg, eta, precision);
				}

				core[v] = etadeg[v];
			}
		}
		*/

		System.out.println("Time elapsed for initial (sec) = " + (System.currentTimeMillis() - startTime) / 1000.0);

	}
	
	
	private void computeInitialEtaDegs_inProcessor(int processor) {
		//computing range of nodes for which to compute initial eta-deg
		int[] range = getRangeOfNodes(processor);
		
		ArcLabelledImmutableGraph H = G.copy();
		
		for (int v = range[0]; v < range[1]; v++) {
			int v_deg = H.outdegree(v);
	
			if (v_deg == 0) {
				etadeg[v] = 0;
			} else {
				Label[] v_labels = H.labelArray(v);

				if (v_deg < L) {
					if(DPtype.equals("DP_log2"))
						etadeg[v] = KCoins.DP_log2(v_labels, v_deg, v_deg, eta, precision);
					else
						etadeg[v] = KCoins.DP(v_labels, v_deg, v_deg, eta, precision);
				} else {
					etadeg[v] = KCoins.LYc(v_labels, v_deg, v_deg, eta, precision);
				}

				core[v] = etadeg[v];
			}
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
	
	
	

	void update(int v) {
		if (iteration == 0) {

			core[v] = etadeg[v];
			scheduled[v] = true;
			change = true;
		}
		int d_v = etadeg[v];
		int[] N_v = G.successorArray(v);
		int l = N_v.length;

		int localEstimate = computeUpperBound(v, d_v, N_v);

		if (localEstimate < core[v]) {
			core[v] = localEstimate;

			change = true;

			for (int i = 0; i < l; i++) {
				int u = N_v[i];

				if (core[v] <= core[u]) {
					scheduled[u] = true;

					if (verificationFlage == true) {
						verificationRequired[u] = true;
					}
				}
			}
		}

	}

	int computeUpperBound(int v, int d_v, int[] N_v) {
		int[] c = new int[core[v] + 1];
		int v_deg = G.outdegree(v);
		for (int i = 0; i < v_deg; i++) {
			int u = N_v[i];
			int j = Math.min(core[v], core[u]);
			c[j]++;
		}

		int cumul = 0;
		for (int i = core[v]; i >= 1; i--) {
			cumul = cumul + c[i];
			if (cumul >= i)
				return i;
		}
		return d_v;
	}

	int DP_modified(Label[] u_labels, int H, int J, boolean array_of_probabilities_available, int precision) {

		if (array_of_probabilities_available == false) {
			double etaProbability = 1.0;
			double[][] dp = new double[H + 1][2];
			int sw = 0;
			boolean find = false;

			prob_values = new double[J + 1];

			for (int j = 0; j <= J; j++) { // column

				for (int h = 0; h <= H; h++) { // row
					if (j == 0 && h == 0) {
						dp[h][sw] = 1;
					} else if (h < j) {
						dp[h][sw] = 0;
					} else {
						double prob = u_labels[h - 1].getLong() * (1.0 / Math.pow(10, precision));
						dp[h][sw] = prob * (j == 0 ? 0 : dp[h - 1][1 - sw]) + (1 - prob) * dp[h - 1][sw];
					}
				}

				double probability = dp[H][sw]; // Pr[deg(v) =j]

				etaProbability -= probability;
				prob_values[j] = etaProbability;

				if (etaProbability < eta && find == false) {
					core_value = j;
					find = true;
				}
				sw = 1 - sw;
			}

			if (find == false) {
				if (J == H) {
					core_value = H;
				} else {
					core_value = J;
				}
				find = true;
			}
			return core_value;
		} else {
			boolean find = false;
			double[] probabilityValues = prob_values;
			prob_values = new double[J + 1]; // stores all the probability
												// values

			double[][] dp = new double[H + 1][2];
			double prob = 0.0;
			int sw = 0;

			for (int j = 0; j <= J; j++) { // Pr[deg(v)>=j+1]
				for (int h = 0; h <= H; h++) {
					if (h == 0) {
						dp[h][sw] = probabilityValues[j];
					} else {
						prob = u_labels[h - 1].getLong() * (1.0 / Math.pow(10, precision));
						dp[h][sw] = prob * (j == 0 ? 1 : dp[h - 1][1 - sw])
								+ (1 - prob) * (j == 0 ? dp[h - 1][sw] : dp[h - 1][sw]);
					}
				}
				double probability = dp[H][sw];
				prob_values[j] = probability;
				sw = 1 - sw;
				if (probability < eta && find == false) {
					core_value = j;
					find = true;
				}

			}
			if (find == false) {
				if (J == H) {
					core_value = H;
				} else {
					core_value = J;
				}
				find = true;
			}
			return core_value;
		}

	}

	int DP_modified_log2(Label[] u_labels, int H, int J, boolean array_of_probabilities_available, int precision) {

		double epsilon = 0.00000000001;

		if (array_of_probabilities_available == false) {
			double etaProbability = 1.0;
			double[][] dp = new double[H + 1][2];
			int sw = 0;
			boolean find = false;

			prob_values = new double[J + 1];

			for (int j = 0; j <= J; j++) { // column

				for (int h = 0; h <= H; h++) { // row
					if (j == 0 && h == 0) {
						dp[h][sw] = 1;
					} else if (h < j) {
						dp[h][sw] = 0;
					} else {
						double prob = (1.0 / Math.pow(10, precision)) * u_labels[h - 1].getLong();
						if (j == 0) {
							dp[h][sw] = prob * 0;
						} else {
							if (prob < epsilon || dp[h - 1][1 - sw] < epsilon)
								dp[h][sw] = 0;
							else
								// dp[h][sw] = Math.pow(2,
								// (DoubleMath.log2(prob) + DoubleMath.log2(dp[h
								// - 1][1 - sw])));
								dp[h][sw] = Math.pow(2, (DoubleMath.log2(prob) + DoubleMath.log2(dp[h - 1][1 - sw])));
							// prob * dp[h - 1][1 - sw];
						}
						if (1 - prob < epsilon || dp[h - 1][sw] < epsilon)
							dp[h][sw] += 0;
						else

							dp[h][sw] += Math.pow(2, (DoubleMath.log2(1 - prob) + DoubleMath.log2(dp[h - 1][sw])));
						// (1 - prob) * dp[h - 1][sw];

					}
				}

				double probability = dp[H][sw]; // Pr[deg(v) =j]

				etaProbability -= probability;
				prob_values[j] = etaProbability;

				if (etaProbability < eta && find == false) {
					core_value = j;
					find = true;
				}
				sw = 1 - sw;
			}

			if (find == false) {
				if (J == H) {
					core_value = H;
				} else {
					core_value = J;
				}
				find = true;
			}
			return core_value;
		} else {
			boolean find = false;
			double[] probabilityValues = prob_values;
			prob_values = new double[J + 1]; // stores all the probability
												// values

			double[][] dp = new double[H + 1][2];
			double prob = 0.0;
			int sw = 0;

			for (int j = 0; j <= J; j++) { // Pr[deg(v)>=j+1]
				for (int h = 0; h <= H; h++) {
					if (h == 0) {
						dp[h][sw] = probabilityValues[j];
					} else {
						prob = u_labels[h - 1].getLong() * (1.0 / Math.pow(10, precision));
						if (j == 0)
							dp[h][sw] = prob;
						else {
							if (prob < epsilon || dp[h - 1][1 - sw] < epsilon)
								dp[h][sw] = 0;
							else
								dp[h][sw] = Math.pow(2, (DoubleMath.log2(prob) + DoubleMath.log2(dp[h - 1][1 - sw])));
							// prob * dp[h - 1][1 - sw];
						}
						if (1 - prob < epsilon || dp[h - 1][sw] < epsilon)
							dp[h][sw] += 0;
						else
							dp[h][sw] += Math.pow(2, (DoubleMath.log2(1 - prob) + DoubleMath.log2(dp[h - 1][sw])));

					}
				}
				double probability = dp[H][sw];
				prob_values[j] = probability;
				sw = 1 - sw;
				if (probability < eta && find == false) {
					core_value = j;
					find = true;
				}

			}
			if (find == false) {
				if (J == H) {
					core_value = H;
				} else {
					core_value = J;
				}
				find = true;
			}
			return core_value;
		}

	}



	// k is the core number neighbors need to have in order to be considered
	int recompute(int[] v_neighbors, Label[] v_label, int v_deg, int k) {
		int v_deg_real;

		// rebuild label_array for v
		v_deg_real = 0; // real degree, i.e. num of neighbors still alive
		for (int t = 0; t < v_deg; t++) {
			int u = v_neighbors[t];

			if (core[u] >= k)
				v_deg_real++;
		}

		int Index = 0;

		Label[] v_label_real = new Label[v_deg_real];

		for (int t = 0; t < v_deg; t++) {
			int u = v_neighbors[t];

			if (core[u] >= k) {
				v_label_real[Index] = v_label[t];
				Index++;
			}
		}
		num_of_edges_alive = v_deg_real;

		if (DPtype.equals("DP_log2"))
			return DP_modified_log2(v_label_real, v_deg_real, k, false, precision);
		else
			return DP_modified(v_label_real, v_deg_real, k, false, precision);
	}

	public int[] Verification(boolean[] b) {
		int localValue;

		for (int u = 0; u < n; u++) {

			if (b[u] == true) {

				int max = 0;
				int num = 0;
				int[] u_neighbors = G.successorArray(u);
				int length_u_neighbors = u_neighbors.length;

				for (int j = 0; j < length_u_neighbors; j++) {
					int neigh = u_neighbors[j];
					if (core[neigh] < core[u]) {
						num++;
					}
				}

				Label[] N = G.labelArray(u);

				int le = G.outdegree(u);
				localValue = recompute(u_neighbors, N, le, core[u]);

				// now prob_values have stored all the probability values which
				// are needed
				if (localValue != core[u]) {
					if (num > 1) {
						max = localValue;

						for (int j = (core[u] - 1); j >= localValue; j--) {
							// double[] prob = prob_values;

							// we should know which neighbors of u has
							// core-number equal to j
							int index = 0;
							int v_deg_real = 0;

							for (int y = 0; y < le; y++) {
								int v = u_neighbors[y];
								if (core[v] == j)
									v_deg_real++;
							}
							Label[] v_label_real = new Label[v_deg_real];
							for (int y = 0; y < le; y++) {
								int v = u_neighbors[y];
								if (core[v] == j) {
									v_label_real[index] = N[y];
									index++;
								}
							}

							int value; 
							if (DPtype.equals("DP_log2"))
								value = DP_modified_log2(v_label_real, v_deg_real, j, true, precision);
							else
								value = DP_modified(v_label_real, v_deg_real, j, true, precision);

							if (value == j) {
								max = value;
								break;
							} else {
								if (value >= max) {
									max = value;
								}
							}

						} // we take the max value

						if (max != core[u]) {
							core[u] = max;
							etadegree_changedFlag = true;
						}

					} else {
						core[u] = localValue;
						etadegree_changedFlag = true;
					}

					// schedule the neighbors of u whose upper-bound is greater
					// or equal to u
					int[] N_i = G.successorArray(u);
					int l = N_i.length;
					for (int j = 0; j < l; j++) {
						int v = N_i[j];
						if (core[v] >= core[u]) {
							scheduled[v] = true;
							verificationRequired[v] = true;
						}
					} // inner for loop
				}

			} // end if
		}
		return core;
	}

	// This is the iterative bound tightening as in the deterministic case.
	public void VCMain() { // the same as deterministic
		while (true) {
			System.out.print("Iteration " + iteration);

			int num_scheduled = 0;
			boolean[] scheduledNow = scheduled.clone();
			for (int v = 0; v < n; v++)
				scheduled[v] = false;

			for (int v = 0; v < n; v++) {
				if (scheduledNow[v] == true) {

					num_scheduled++;
					update(v);
				}
			}
			System.out.println("\t\t" + ((100.0 * num_scheduled) / n) + "%\t of nodes were scheduled this iteration.");
			iteration++;
			if (change == false)
				break;
			else
				change = false;
		} // end while

	}

	// This calls bound-tightening and verification repeatedly until fixed point
	public int[] KCoreCompute() throws IOException {

		VCMain();

		// the first time when we run verification. we should verify all the
		// nodes
		for (int i = 0; i < n; i++) {
			verificationRequired[i] = true;
		}

		etadegree_changedFlag = false;

		while (true) {

			boolean[] verificationRequiredNow = verificationRequired.clone();
			for (int v = 0; v < n; v++)
				verificationRequired[v] = false;

			Verification(verificationRequiredNow);

			if (etadegree_changedFlag == false) {
				break;
			} else {
				verificationFlage = true;

				VCMain();
				etadegree_changedFlag = false;
				numOfverificationSteps++;
			}
		}
		return core;
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
		// args = new String[] { "biomine.w", "0.0","1500", "16"};
		//args = new String[] { "DBLP-proc.w", "0.1", "1500", "16", "DP_log2"};
		//args = new String[] { "Flickr-proc.w", "0.0", "1500", "16",  "DP_log2"};
		//args = new String[] { "Flickr.w", "0.0", "1500", "16" };
		// args = new String[] { "DBLP.w", "0.0","1500", "16"};
		// args = new String[] { "../webgraphs/cnr-2000/cnr-2000-sym-noself.w",
		// "0.0",
		// "1500", "2"};
		// args = new String[] { "uk-2005/uk-2005-sym-noself.w", "0.5", "1500",
		// "2" };
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

		long startTime = System.currentTimeMillis();
		K_VC tt = new K_VC(basename, eta, L, precision, DPtype);
		int[] res = tt.KCoreCompute();

		System.out.println("Number of iterations=" + tt.iteration + "\t" + "Number of verification steps="
				+ tt.numOfverificationSteps);
		System.out.println(args[0] + ": Time elapsed (sec) = " + (System.currentTimeMillis() - startTime) / 1000.0);

		tt.writeResults(res, basename+"eta-" + eta + "-vc.txt");
	}
}
