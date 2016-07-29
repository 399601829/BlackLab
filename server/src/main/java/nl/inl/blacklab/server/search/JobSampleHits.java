package nl.inl.blacklab.server.search;


import nl.inl.blacklab.search.Hits;
import nl.inl.blacklab.search.HitsSample;
import nl.inl.blacklab.server.dataobject.DataObjectMapElement;
import nl.inl.blacklab.server.exceptions.BlsException;

/**
 * Sample hits from a Hits object
 */
public class JobSampleHits extends JobWithHits {

	public static class JobDescSampleHits extends JobDescription {

		SampleSettings sampleSettings;

		public JobDescSampleHits(JobDescription hitsToSample, SampleSettings settings) {
			super(JobSampleHits.class, hitsToSample);
			this.sampleSettings = settings;
		}

		@Override
		public SampleSettings getSampleSettings() {
			return sampleSettings;
		}

		@Override
		public String uniqueIdentifier() {
			return super.uniqueIdentifier() + "[" + sampleSettings + "]";
		}

		@Override
		public DataObjectMapElement toDataObject() {
			DataObjectMapElement o = super.toDataObject();
			o.put("sampleSettings", sampleSettings.toString());
			return o;
		}

	}

	public JobSampleHits(SearchManager searchMan, User user, JobDescription par) throws BlsException {
		super(searchMan, user, par);
	}

	@Override
	public void performSearch() throws BlsException {
		Hits inputHits = ((JobWithHits)inputJob).getHits();
		SampleSettings sample = jobDesc.getSampleSettings();
		if (sample.percentage() >= 0) {
			hits = HitsSample.fromHits(inputHits, sample.percentage() / 100f, sample.seed());
		} else if (sample.number() >= 0) {
			hits = HitsSample.fromHits(inputHits, sample.number(), sample.seed());
		}
	}

}
