
/**
 *  Copyright (c) 2018 Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

import retrieval_model.RetrievalModel;
import retrieval_model.RetrievalModelRankedBoolean;
import retrieval_model.RetrievalModelUnrankedBoolean;

/**
 * The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		return this.docIteratorHasMatchMin(r);
	}

	/**
	 * Get a score for the document that docIteratorHasMatch matched.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScore(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean) {
			return this.getScoreUnrankedBoolean(r);
		}
		if (r instanceof RetrievalModelRankedBoolean) {
			return this.getScoreRankedBoolean(r);
		} else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the OR operator.");
		}
	}
	
	/**
	 * inherit from QrySop
	 */
	public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
		return 0.0;
	}

	/**
	 * getScore for the UnrankedBoolean retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	/**
	 * getScore for the RankedBoolean retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			double maxScore = Double.MIN_VALUE;
			int docid = this.docIteratorGetMatch();
			for (int i = 0; i < this.args.size(); i++) {
				Qry q_i = this.args.get(i);
				if (q_i.docIteratorHasMatchCache()) {
					int id = q_i.docIteratorGetMatch();
					if (id == docid) {
						double score = ((QrySop) this.args.get(i)).getScore(r);
						maxScore = maxScore < score ? score : maxScore;
					}
				}
			}
			return maxScore;
		}
	}

}
