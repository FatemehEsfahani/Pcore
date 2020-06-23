import java.math.BigDecimal;
import java.math.MathContext;

import org.apache.commons.math3.distribution.NormalDistribution;

import com.google.common.math.DoubleMath;

import it.unimi.dsi.webgraph.labelling.Label;

public class KCoins {

	static NormalDistribution norm = new NormalDistribution();
	
	static int DP_BigDecimal(Label[] u_labels, int H, int J, double eta, int precision, int mcbits) {
		MathContext mc;
		if(mcbits == -1)
			mc = MathContext.UNLIMITED;
		else	
			mc = new MathContext(mcbits);
		
		BigDecimal one = new BigDecimal(1.0,mc);
		BigDecimal zero = new BigDecimal(0.0,mc); 
		BigDecimal bigEta = new BigDecimal(eta,mc);
		BigDecimal etaProbability = one;
		BigDecimal[][] dp = new BigDecimal[H + 1][2];
		int sw = 0;

		for (int j = 0; j <= J; j++) { // column

			for (int h = 0; h <= H; h++) { // row
				if (j == 0 && h == 0) {
					dp[h][sw] = one;
				} else if (h < j) {
					dp[h][sw] = zero;
				} else {
					BigDecimal prob = new BigDecimal((1.0 / Math.pow(10, precision)) * u_labels[h - 1].getLong(), mc);
					BigDecimal oneMinus_prob = one.subtract(prob, mc);
					if (j == 0) {
						dp[h][sw] = dp[h - 1][sw].multiply(oneMinus_prob, mc);
					} else {
						dp[h][sw] = dp[h - 1][1 - sw].multiply(prob, mc).add(dp[h - 1][sw].multiply(oneMinus_prob, mc),
								mc);

					}

				}
			}
            
			
			BigDecimal probability = dp[H][sw];
			etaProbability = etaProbability.subtract(probability);
			//System.out.println("big" + etaProbability);

			if (etaProbability.compareTo(bigEta) < 0) {
				return j;
			}
			sw = 1 - sw;
		}

		if (J == H) {
			return H;
		} else {
			return J;
		}

	}
	
	
	static int DP(Label[] u_labels, int H, int J, double eta, int precision) {

		double etaProbability = 1.0;
		double[][] dp = new double[H + 1][2];
		int sw = 0;

		for (int j = 0; j <= J; j++) { // column

			for (int h = 0; h <= H; h++) { // row
				if (j == 0 && h == 0) {
					dp[h][sw] = 1;
				} else if (h < j) {
					dp[h][sw] = 0;
				} else {

					double prob = (1.0 / Math.pow(10, precision)) * u_labels[h - 1].getLong();
					dp[h][sw] = prob * (j == 0 ? 0 : dp[h - 1][1 - sw]) + (1- prob) * dp[h - 1][sw];
				}
			}

			double probability = dp[H][sw];
			

			etaProbability -= probability;

			if (etaProbability < eta) {
				return j;
			}
			sw = 1 - sw;
		}

		if (J == H) {
			return H;
		} else {
			return J;
		}

	}
	
	
	static int DP_log2(Label[] u_labels, int H, int J, double eta, int precision) {

		double etaProbability = 1.0;
		double[][] dp = new double[H + 1][2];
		int sw = 0;

		for (int j = 0; j <= J; j++) { // column

			for (int h = 0; h <= H; h++) { // row
				if (j == 0 && h == 0) {
					dp[h][sw] = 1;
				} else if (h < j) {
					dp[h][sw] = 0;
				} else {

					double prob = (1.0/Math.pow(10, precision))*u_labels[h - 1].getLong();
					
					if(j==0)
						dp[h][sw] = prob * 0;
					else
						dp[h][sw] = Math.pow(2, (DoubleMath.log2(prob) + DoubleMath.log2(dp[h - 1][1 - sw])));
									//prob * dp[h - 1][1 - sw];
								
					dp[h][sw] += Math.pow(2, (DoubleMath.log2(1-prob) + DoubleMath.log2(dp[h - 1][sw]))); 
							//(1 - prob) * dp[h - 1][sw];
							
				}
			}

			double probability = dp[H][sw];

			etaProbability -= probability;

			if (etaProbability < eta) {
				return j;
			}
			sw = 1 - sw;
		}

		if (J == H) {
			return H;
		} else {
			return J;
		}

	}
	
	
	static int DP_log2_new_Alex(Label[] u_labels, int H, int J, double eta, int precision) {
		
		double etaProbability = 1.0;
		double[][] dp = new double[H + 1][2];
		int sw = 0;

		for (int j = 0; j <= J; j++) { // column

			for (int h = 0; h <= H; h++) { // row
				if (j == 0 && h == 0) {
					dp[h][sw] = 0;
					//dp[h][sw] = 1;
				} else if (h < j) {
					dp[h][sw] = Double.NEGATIVE_INFINITY;
					//dp[h][sw] = 0;
				} else {

					double prob = (1.0/Math.pow(10, precision))*u_labels[h - 1].getLong();
					double probL = DoubleMath.log2( prob );
					double one_minus_probL = DoubleMath.log2( 1-prob ); 
					//double prob = (1.0/Math.pow(10, precision))*u_labels[h - 1].getLong();
					
					if(j==0)
						dp[h][sw] = Double.NEGATIVE_INFINITY;
						//dp[h][sw] = prob * 0;
					else
						dp[h][sw] = probL + dp[h - 1][1 - sw];
						//dp[h][sw] = Math.pow(2, (DoubleMath.log2(prob) + DoubleMath.log2(dp[h - 1][1 - sw])));
									//prob * dp[h - 1][1 - sw];
					
					double x_p = dp[h][sw];
					double y_p = one_minus_probL + dp[h - 1][sw];
					dp[h][sw] = DoubleMath.log2(Math.pow(2,x_p) + Math.pow(2,y_p)); 
						//x_p+DoubleMath.log2(1+Math.pow(2,y_p-x_p));
					
					//dp[h][sw] += Math.pow(2, (DoubleMath.log2(1-prob) + DoubleMath.log2(dp[h - 1][sw]))); 
							//(1 - prob) * dp[h - 1][sw];
							
				}
			}

			double probability = Math.pow(2,dp[H][sw]);
			//double probability = dp[H][sw];

			etaProbability -= probability;

			if (etaProbability < eta) {
				return j;
			}
			sw = 1 - sw;
		}

		if (J == H) {
			return H;
		} else {
			return J;
		}

	}
	
	
	static public int LYc(Label[] LabelArray, int dim, int J, double eta, int precision) {
		double s_n2 = 0.0;
		double expectedValue = 0.0;
		double s_n;
		double z;
		int core = 0;

		Label[] N_v_probValues = LabelArray;

		// the for-loop computes sn^2 and the sum of mu_i
		for (int j = 0; j < N_v_probValues.length; j++) {
			double prob_value = N_v_probValues[j].getLong() * (1.0 / Math.pow(10, precision));
			s_n2 = s_n2 + (prob_value * (1 - prob_value));
			expectedValue = expectedValue + (prob_value);
		}

		s_n = Math.sqrt(s_n2);
		// computes initial eta-degrees

		double prob;
		// int degree = G.outdegree(v);

		for (int k = 1; k <= J; k++) {
			z = (double) ((k - 0.5) - expectedValue) / s_n;
			prob = 1 - norm.cumulativeProbability(z);
			

			if (prob < eta) {
				core = k - 1;
				break;
			}
			if (k == J) {
				core = J;
			}

		}

		return core;
	}
	
	static public int LYc_invc(Label[] LabelArray, int dim, int J, double eta, int precision) {

		int k_pp = LYinvc(LabelArray, dim, J, eta, precision);

		double s_n2 = 0.0;
		double expectedValue = 0.0;
		double s_n;
		double z;
		int core = 0;
		

		Label[] N_v_probValues = LabelArray;

		// the for-loop computes sn^2 and the sum of mu_i
		for (int j = 0; j < N_v_probValues.length; j++) {
			double prob_value = N_v_probValues[j].getLong() * (1.0 / Math.pow(10, precision));
			s_n2 = s_n2 + (prob_value * (1 - prob_value));
			expectedValue = expectedValue + (prob_value);
		}

		s_n = Math.sqrt(s_n2);
		// computes initial eta-degrees

		double prob;
		// int degree = G.outdegree(v);

		for (int k = Math.max(0, k_pp - 2); k <= Math.min(k_pp + 2, dim); k++) {
			z = (double) ((k - 0.5) - expectedValue) / s_n;
			prob = 1 - norm.cumulativeProbability(z);

			if (prob < eta) {
				core = k - 1;
				break;
			}
			if (k == J) {
				core = J;
			}

		}

		return core;
	}

	static public int LYinvc(Label[] LabelArray, int dim, int J, double eta, int precision) {
		double s_n2;
		double expectedValue;
		double s_n;
		double z;
		

		s_n2 = 0.0;
		expectedValue = 0.0;
		Label[] N_v_probValues = LabelArray;

		// the for-loop computes sn^2 and the sum of mu_i
		for (int j = 0; j < N_v_probValues.length; j++) {
			double prob_value = N_v_probValues[j].getLong() * (1.0 / Math.pow(10, precision));
			s_n2 = s_n2 + (prob_value * (1 - prob_value));
			expectedValue = expectedValue + (prob_value);
		}

		s_n = Math.sqrt(s_n2);

		z = norm.inverseCumulativeProbability(1 - eta);

		int r = (int) Math.round(z * s_n + expectedValue - 1);
		if (r < 0) {
			return 0;
		} else {
			return r;
		}
	}
	

}
