import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

public class Estimator {

	private HashMap<String, HashMap<String, Double>> sigma;
	private HashMap<String, HashMap<String, Double>> tau;
	private HashSet<String> tags;
	private HashSet<String> words;
	private HashMap<String, Double> initials;
	public static double param = +0.7;

	public Estimator() {
		this.sigma = new HashMap<String, HashMap<String, Double>>();
		this.tau = new HashMap<String, HashMap<String, Double>>();
		this.tags = new HashSet<String>();
		this.words = new HashSet<String>();
		this.initials = new HashMap<String, Double>();
	}

	public static void main(String[] args) throws IOException {


		File file1 = new File(args[0]);
		File file2 = new File(args[1]);
		File file3 = new File(args[2]);

		Estimator estimator = new Estimator();
		estimator.getEstimates(file1);
		estimator.doViterbi(file2,file3);
	}

	public void getEstimates(File file) throws FileNotFoundException {

		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			String[] temp = line.split(" ");
			Double oi = this.initials.get(temp[1]);
			this.initials.put(temp[1], (oi != null) ? oi + 1.0 : 1.0);

			for (int i = 0; i <= temp.length - 1; i = i + 2) {
				String word = temp[i];
				String state = temp[i + 1];
				this.words.add(word);
				this.tags.add(state);

				if (this.tau.containsKey(state)) {
					HashMap<String, Double> hm = this.tau.get(state);
					Double o = hm.get(word);
					hm.put(word, (o != null) ? o.doubleValue() + 1 : 1.0);
					this.tau.put(state, hm);
				} else {
					HashMap<String, Double> hm = new HashMap<String, Double>();
					hm.put(word, 1.0);
					hm.put("*UNK*", 0.0);
					this.tau.put(state, hm);
				}
			}
			this.words.add("*UNK*");
			
			for (int i = 1; i <= temp.length - 3; i = i + 2) {
				String state_prev = temp[i];
				String state_next = temp[i + 2];

				if (this.sigma.containsKey(state_prev)) {
					HashMap<String, Double> hm = this.sigma.get(state_prev);
					Double o = hm.get(state_next);
					hm.put(state_next, (o != null) ? o.doubleValue() + 1.0 : 1.0);
					this.sigma.put(state_prev, hm);
				} else {
					HashMap<String, Double> hm = new HashMap<String, Double>();
					hm.put(state_next, 1.0);
					this.sigma.put(state_prev, hm);
				}
			}
		}
		//looping over sentences end
		scanner.close();
		
		
		
		double ini_sum = 0.0;
		for (Map.Entry<String, Double> entry : this.initials.entrySet()) {
			ini_sum += entry.getValue();
		}
		for (String ini_tag : this.tags) {
			if (this.initials.containsKey(ini_tag)) {
				this.initials.put(ini_tag, Math.log(this.initials.get(ini_tag) + 1) - Math.log(ini_sum + this.tags.size()));
			} else {
				this.initials.put(ini_tag, -Math.log(ini_sum + this.tags.size()));
			}
		}

		for (Map.Entry<String, HashMap<String, Double>> entry : this.tau.entrySet()) {
			String state = entry.getKey();
			HashMap<String, Double> hm = entry.getValue();

			double sum = 0.0;
			for (Map.Entry<String, Double> entry_in : hm.entrySet()) {
				sum += entry_in.getValue();
			}

			for (Map.Entry<String, Double> entry_in : hm.entrySet()) {
				String word = entry_in.getKey();
				Double num = entry_in.getValue();
				hm.put(word, Math.log(num+param) - Math.log(sum+this.words.size()));
			}
			this.tau.put(state, hm);////
		}

		for (String pre : this.tags) {// smooth
			if (this.sigma.get(pre) == null) {
				HashMap<String, Double> hm = new HashMap<String, Double>();
				this.sigma.put(pre, hm);
			}
			for (String pos : this.tags) {
				if (this.sigma.get(pre).get(pos) == null) {
					HashMap<String, Double> hm = this.sigma.get(pre);
					hm.put(pos, 0.0);
					this.sigma.put(pre, hm);
				}
			}
		}
		
		for (Map.Entry<String, HashMap<String, Double>> entry : this.sigma.entrySet()) {
			String state_prev = entry.getKey();
			HashMap<String, Double> hm = entry.getValue();

			double sum = 0.0;
			for (Map.Entry<String, Double> entry_in : hm.entrySet()) {
				Double num = entry_in.getValue();
				sum += num;
			}
			sum += hm.size();

			for (Map.Entry<String, Double> entry_in : hm.entrySet()) {
				String state_next = entry_in.getKey();
				Double num = entry_in.getValue();
				hm.put(state_next, Math.log(num + 1.0) - Math.log(sum));
			}
			this.sigma.put(state_prev, hm);
		}
	}

	
	public void doViterbi(File file, File out) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(out)), false);
		
		Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)));
		double hit = 0;
		double sum = 0;

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			String[] temp = line.split(" ");

			HashMap<Integer, HashMap<String, Double>> miu = new HashMap<Integer, HashMap<String, Double>>();
			HashMap<String, Double> start_miu = new HashMap<String, Double>();

			for (Map.Entry<String, Double> entry : this.initials.entrySet()) {
				if (this.tau.get(entry.getKey()).get(temp[0]) != null) {
					start_miu.put(entry.getKey() + " " + "#start#", entry.getValue() + this.tau.get(entry.getKey()).get(temp[0]));
				} else {
					start_miu.put(entry.getKey() + " " + "#start#", entry.getValue() + this.tau.get(entry.getKey()).get("*UNK*"));
				}
			}
			miu.put(1, start_miu);

			for (int i = 3; i <= temp.length - 1; i = i + 2) {
				HashMap<String, Double> temp_hm = new HashMap<String, Double>();

				for (String cur_tag : this.tags) {
					double max_probability = Double.NEGATIVE_INFINITY;
					String prev_tag = null;
					for (Map.Entry<String, Double> entry : miu.get(i - 2).entrySet()) {
						String poss_prev_tag = entry.getKey().split(" ")[0];
						double miu_probability = entry.getValue();
						
						double sigma_probability = this.sigma.get(poss_prev_tag).get(cur_tag).doubleValue();
						
						Double o2 = this.tau.get(cur_tag).get(temp[i - 1]);
						double tau_probability = ((o2 != null) ? o2 : (this.tau.get(cur_tag).get("*UNK*")));
 
						double cur_probability = miu_probability + sigma_probability + tau_probability;

						if (cur_probability > max_probability) {
							max_probability = cur_probability;
							prev_tag = poss_prev_tag;
						}
					}
					temp_hm.put(cur_tag + " " + prev_tag, max_probability);
				}
				miu.put(i, temp_hm);
			}

			HashMap<Integer, String> estimated_tag = new HashMap<Integer, String>();

			HashMap<String, Double> last_tags = miu.get(temp.length - 1);
			String final_tag = null;
			double max_final_prob = Double.NEGATIVE_INFINITY;
			for (Map.Entry<String, Double> entry : last_tags.entrySet()) {
				if (entry.getValue() > max_final_prob) {
					final_tag = entry.getKey().split(" ")[0];
					max_final_prob = entry.getValue();
				}
			}
			estimated_tag.put(temp.length - 1, final_tag);

			for (int i = temp.length - 1; i >= 3; i = i - 2) {
				double cur_max_prob = Double.NEGATIVE_INFINITY;
				String cur_tag = null;

				for (Map.Entry<String, Double> entry : miu.get(i).entrySet()) {
					String poss = entry.getKey().split(" ")[0].trim();
					if (poss.equals(estimated_tag.get(i).trim())) {
						double prob = entry.getValue();
						if (prob > cur_max_prob) {// CARE
							cur_max_prob = prob;
							cur_tag = entry.getKey().split(" ")[1].trim();
						}
					}
				}
				estimated_tag.put(i - 2, cur_tag);
			}

			for (int i = 1; i <= temp.length - 1; i = i + 2) {
				pw.print(temp[i-1]+" "+estimated_tag.get(i)+" ");
				pw.flush();
				if (estimated_tag.get(i).trim().equals(temp[i].trim())) {
					hit++;
				}
			}
			sum += (temp.length / 2);
			pw.println("");
		}

		//System.out.println("hit " + hit + " sum: " + sum);
		//System.out.println("hit rate " + hit / sum);
		scanner.close();
		pw.close();
	}

}
