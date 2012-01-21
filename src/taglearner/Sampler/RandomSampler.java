package taglearner.Sampler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import weka.clusterers.RandomizableClusterer;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;

public class RandomSampler {
	
	public List<Integer> getSamples(Set<Integer> cluster, int n) {

		ArrayList<Integer> sample = new ArrayList<Integer>(n);
		TreeSet<Integer> tmp = new TreeSet<Integer>();
		
		
		for (int i=0; i<n; ++i) {
			Calendar d = new GregorianCalendar();
			Random r = new Random();
			
			// the cluster set changes every iteration
			// so reset the seed this way
			r.setSeed(d.getTimeInMillis());
			Iterator<Integer> iter = cluster.iterator();

			int size = cluster.size();
			int steps = r.nextInt(size);
			int start = 0;
			int pick = 0;
			
			// traverse random number of steps and select
			do {
				pick = iter.next();
				++start;
			} while (start < steps);
			
			// remove the selected member from the set
			// so it's not selected again
			if (!cluster.remove((Object) pick)) {
				System.err.println("ERROR: Selected non-existent member!!!");
			}
			
			// add the selected member to the sample list
			//sample.add(pick);
			tmp.add(pick);
		}
		
		sample.addAll(tmp);
		return sample;
	}
	
	public ArrayList<ArrayList<Integer>> getSamples(RandomizableClusterer myCluster, Instances instances, int n) {
		ArrayList<ArrayList<Integer>> samples = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> sample;
		int k = 5;
		try {
			k = myCluster.numberOfClusters();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SimpleKMeans kmeans = (SimpleKMeans) myCluster;
		int[] assignments = {0};
		try {
			assignments = kmeans.getAssignments();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		int size = instances.numInstances();
		ArrayList<TreeSet<Integer>> clusterMap = new ArrayList<TreeSet<Integer>>(k);
		for (int i=0; i<k; ++i) {
			TreeSet<Integer> cluster = new TreeSet<Integer>();
			clusterMap.add(cluster);
		}

		for (int i=0; i<size; ++i) {
			clusterMap.get(assignments[i]).add(i);
		}
		
		for (int j=0; j<k; ++j) {
			sample = (ArrayList<Integer>) getSamples(clusterMap.get(j),n);
			samples.add(sample);
		}
		
		System.out.println(samples.toString());
		
		return samples;
	}
}
