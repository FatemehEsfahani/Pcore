import org.apache.commons.math3.distribution.ZipfDistribution;

import it.unimi.dsi.webgraph.labelling.GammaCodedLongLabel;
import it.unimi.dsi.webgraph.labelling.Label;

public class KTest {
	
	static int zipfNumberOfElements = 100;
	static int zipfExponent = 1;
	
	
	public static void main(String[] args) throws Exception {
		
		for(double eta = 0.0; eta<=0.5; eta+=0.1) {
			
			test(500, 25, eta,16,"uniform");
			test(500, 50, eta,16,"uniform");
			test(200, 75, eta,16,"uniform");
			test(100, 100,eta,16,"uniform");
			test(100, 500, eta,16,"uniform");
			test(100, 1000,eta,16,"uniform");
			test(100, 1500,eta,16,"uniform");
		}
	}

	public static void test(
			int number_of_tests, 
			int length_of_array, 
			double eta, 
			int precision, 
			String distribution) throws Exception {
		
		//How many coins are head with a probability at least eta?
		int 
			DP_BigDec, //unlimited precision
			DP,
			DP_BigDec32,
			DP_BigDec64,
			DP_BigDec128,
			DP_BigDec256,
			DP_log2,
			LYc;
		
		double DP_BigDec_sum = 0;
		
		int 
			DP_errors = 0,
			DP_BigDec32_errors = 0,
			DP_BigDec64_errors = 0,
			DP_BigDec128_errors = 0,
			DP_BigDec256_errors = 0,
			DP_log2_errors = 0,
			LYc_errors = 0;
		
		double 
			DP_errors_sum = 0,
			DP_BigDec32_errors_sum = 0,
			DP_BigDec64_errors_sum = 0,
			DP_BigDec128_errors_sum = 0,
			DP_BigDec256_errors_sum = 0,
			DP_log2_errors_sum = 0,
			LYc_errors_sum = 0;
		
		double 	DP_BigDec_time_sum = 0,
				DP_time_sum = 0, 
				DP_BigDec32_time_sum = 0,
				DP_BigDec64_time_sum = 0,
				DP_BigDec128_time_sum = 0,
				DP_BigDec256_time_sum = 0,
				DP_log2_time_sum = 0, 
				LYc_time_sum=0;
		

		long maxWeight = (long) Math.pow(10, precision);
		
		Label[] ll = new Label[length_of_array];
		long startTime;
		
		for (int n = 0; n < number_of_tests; n++) {
			//System.out.println("Test "+n);
			
			ZipfDistribution zipf = null;
			if(distribution.equals("pareto")) 
				zipf = new ZipfDistribution(zipfNumberOfElements,zipfExponent);
			
			for (int i = 0; i < ll.length; i++) {
				GammaCodedLongLabel prototype = new GammaCodedLongLabel("FOO");
				GammaCodedLongLabel label = prototype.copy();
				
				label.value = 0;
				if(distribution.equals("pareto")) 
					label.value = (long) (maxWeight * (1.0*zipf.sample()/zipfNumberOfElements));
				else
					while(label.value == 0)
						label.value = (long) (maxWeight * Math.random());
				
				ll[i] = label;
			}
			
			startTime = System.currentTimeMillis();
			DP_BigDec = KCoins.DP_BigDecimal(ll, ll.length, ll.length, eta, precision, -1);
			DP_BigDec_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP = KCoins.DP(ll, ll.length, ll.length, eta, precision);
			DP_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP_BigDec32 = KCoins.DP_BigDecimal(ll, ll.length, ll.length, eta, precision, 32);
			DP_BigDec32_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP_BigDec64 = KCoins.DP_BigDecimal(ll, ll.length, ll.length, eta, precision, 64);
			DP_BigDec64_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP_BigDec128 = KCoins.DP_BigDecimal(ll, ll.length, ll.length, eta, precision, 128);
			DP_BigDec128_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP_BigDec256 = KCoins.DP_BigDecimal(ll, ll.length, ll.length, eta, precision, 256);
			DP_BigDec256_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			DP_log2 = KCoins.DP_log2(ll, ll.length, ll.length, eta, precision);
			DP_log2_time_sum += System.currentTimeMillis() - startTime;
			
			startTime = System.currentTimeMillis();
			LYc = KCoins.LYc(ll, ll.length, ll.length, eta, precision);
			LYc_time_sum += System.currentTimeMillis() - startTime;
			
			if(DP != DP_BigDec)
				DP_errors++;
			
			if(DP_BigDec32 != DP_BigDec)
				DP_BigDec32_errors++;
			
			if(DP_BigDec64 != DP_BigDec)
				DP_BigDec64_errors++;
			
			if(DP_BigDec128 != DP_BigDec)
				DP_BigDec128_errors++;
			
			if(DP_BigDec256 != DP_BigDec)
				DP_BigDec256_errors++;

			if(DP_log2 != DP_BigDec)
				DP_log2_errors++;
			
			if(LYc != DP_BigDec) 
				LYc_errors++;
			
			
			DP_errors_sum += 1.0*Math.abs(DP-DP_BigDec)/DP_BigDec;
			DP_BigDec32_errors_sum += 1.0*Math.abs(DP_BigDec32-DP_BigDec)/DP_BigDec;
			DP_BigDec64_errors_sum += 1.0*Math.abs(DP_BigDec64-DP_BigDec)/DP_BigDec;
			DP_BigDec128_errors_sum += 1.0*Math.abs(DP_BigDec128-DP_BigDec)/DP_BigDec;
			DP_BigDec256_errors_sum += 1.0*Math.abs(DP_BigDec256-DP_BigDec)/DP_BigDec;
			DP_log2_errors_sum += 1.0*Math.abs(DP_log2-DP_BigDec)/DP_BigDec;
			LYc_errors_sum += 1.0*Math.abs(LYc-DP_BigDec)/DP_BigDec;
			
			DP_BigDec_sum += DP_BigDec;
			
		}

		System.out.println(
				"\teta\t" + eta +
				"\tprecision\t" + precision +
				"\tarray\t" + length_of_array + 
				"\tavg eta-deg\t" + 1.0*DP_BigDec_sum/number_of_tests);
		
		System.out.println("\t" + 
				"DP\t" +
				"DP32\t" +	
				"DP64\t" +
				"DP128\t" +
				"DP256\t" +
				"DPU\t" +
				"DPlog2\t" +
				"LYc\t");
		
		System.out.println( "number of errors (%)" + "\t" +
				1.0*DP_errors/number_of_tests + "\t" +
				1.0*DP_BigDec32_errors/number_of_tests + "\t" +
				1.0*DP_BigDec64_errors/number_of_tests + "\t" +
				1.0*DP_BigDec128_errors/number_of_tests + "\t" +
				1.0*DP_BigDec256_errors/number_of_tests + "\t" +
				0.0 + "\t" +
				1.0*DP_log2_errors/number_of_tests + "\t" +
				1.0*LYc_errors/number_of_tests); 
		
		System.out.println( "avg relative error (%)" + "\t" +
				1.0*DP_errors_sum/number_of_tests + "\t" +
				1.0*DP_BigDec32_errors_sum/number_of_tests + "\t" +
				1.0*DP_BigDec64_errors_sum/number_of_tests + "\t" +
				1.0*DP_BigDec128_errors_sum/number_of_tests + "\t" +
				1.0*DP_BigDec256_errors_sum/number_of_tests + "\t" +
				0.0 + "\t" +
				1.0*DP_log2_errors_sum/number_of_tests + "\t" +
				1.0*LYc_errors_sum/number_of_tests); 
		
		System.out.println( "avg time (ms)" + "\t" +
				1.0*DP_time_sum/number_of_tests + "\t" +
				1.0*DP_BigDec32_time_sum/number_of_tests + "\t" +
				1.0*DP_BigDec64_time_sum/number_of_tests + "\t" +
				1.0*DP_BigDec128_time_sum/number_of_tests + "\t" +
				1.0*DP_BigDec256_time_sum/number_of_tests + "\t" +
				1.0*DP_BigDec_time_sum/number_of_tests + "\t" +
				1.0*DP_log2_time_sum/number_of_tests + "\t" +
				1.0*LYc_time_sum/number_of_tests);
		
		System.out.println();
	}
}
