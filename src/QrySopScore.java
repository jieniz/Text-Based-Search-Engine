
import java.io.*;
import java.lang.IllegalArgumentException;

import retrieval_model.RetrievalModel;
import retrieval_model.RetrievalModelBM25;
import retrieval_model.RetrievalModelIndri;
import retrieval_model.RetrievalModelRankedBoolean;
import retrieval_model.RetrievalModelUnrankedBoolean;
import support.Idx;

/**
 * The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

	/**
	 * Document-independent values that should be determined just once. Some
	 * retrieval models have these, some don't.
	 */

	/**
	 * Indicates whether the query has a match.
	 * 
	 * @param r The retrieval model that determines what is a match
	 * @return True if the query matches, otherwise false.
	 */
	public boolean docIteratorHasMatch(RetrievalModel r) {
		return this.docIteratorHasMatchFirst(r);
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
		} else if (r instanceof RetrievalModelRankedBoolean) {
			return this.getScoreRankedBoolean(r);
		} else if (r instanceof RetrievalModelBM25) {
			return this.getScoreBM25(r);
		} else if (r instanceof RetrievalModelIndri) {
            return this.getScoreIndri(r);
        } else {
			throw new IllegalArgumentException(r.getClass().getName() + " doesn't support the SCORE operator.");
		}
	}

	/**
	 * getScore for the Unranked retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScoreUnrankedBoolean(RetrievalModel r) throws IOException {
		if (!this.docIteratorHasMatchCache()) {
			return 0.0;
		} else {
			return 1.0;
		}
	}

	/**
	 * getScore for the Ranked retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScoreRankedBoolean(RetrievalModel r) throws IOException {
		QryIop qry = this.getArg(0);
		return qry.docIteratorGetMatchPosting().tf;
	}

	/**
	 * getScore for the BM25 retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScoreBM25(RetrievalModel r) throws IOException {
		QryIop qry = this.getArg(0);
		double k_1 = ((RetrievalModelBM25) r).getK1();
        double k_3 = ((RetrievalModelBM25) r).getK3();
        double b = ((RetrievalModelBM25) r).getB();
        
		if (!qry.docIteratorHasMatch(r)) {
			return 0.0;
		} else {
			// calculate idf
            double df = qry.getDf();
            double idf = Math.log((Idx.getNumDocs() - df + 0.5) / (df + 0.5));
            if(idf < 0.0) idf = 0.0;
            
            // calculate tf weight
            double tf = qry.docIteratorGetMatchPosting().tf;
            double doclen = Idx.getFieldLength(qry.getField(), qry.docIteratorGetMatch());
            double avg_doclen = Idx.getSumOfFieldLengths(qry.getField()) / (double) Idx.getDocCount(qry.getField());
            double tf_weight = tf / (tf + k_1 * ((1 - b) + b * doclen / avg_doclen));
            
            // calculate user weight
            double user_weight = (k_3 + 1.0) * 1 / (k_3 + 1);
            
            return idf * tf_weight * user_weight;
		}
	}
	
	/**
	 * get the default score for the indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
        QryIop qry = this.getArg(0);
        double mu = ((RetrievalModelIndri) r).getMu();
        double lambda = ((RetrievalModelIndri) r).getLambda();
        
        double doclen = Idx.getFieldLength(qry.getField(), docid);
        double pmle = qry.getCtf() / (double) Idx.getSumOfFieldLengths(qry.getField());

        return (1.0 - lambda) * ((0 + mu * pmle) / (doclen + mu)) + lambda * pmle;
    }
	
	/**
	 * get the score for the indri retrieval model.
	 * 
	 * @param r The retrieval model that determines how scores are calculated.
	 * @param docid The document id.
	 * @return The document score.
	 * @throws IOException Error accessing the Lucene index
	 */
	public double getScoreIndri(RetrievalModel r) throws IOException {
		QryIop qry = this.getArg(0);
        double mu = ((RetrievalModelIndri) r).getMu();
        double lambda = ((RetrievalModelIndri) r).getLambda();
        
        if (!qry.docIteratorHasMatch(r)) {
        	return 0.0;
        } else {
        	double tf = qry.docIteratorGetMatchPosting().tf;
            
            double doclen = Idx.getFieldLength(qry.getField(), qry.docIteratorGetMatch());
            double pmle = qry.getCtf() / (double) Idx.getSumOfFieldLengths(qry.getField());

            return ((1.0 - lambda) * ((tf + mu * pmle) / (doclen + mu)) + lambda * pmle);
        }
    }
	
	/**
	 * Initialize the query operator (and its arguments), including any internal
	 * iterators. If the query operator is of type QryIop, it is fully evaluated,
	 * and the results are stored in an internal inverted list that may be accessed
	 * via the internal iterator.
	 * 
	 * @param r A retrieval model that guides initialization
	 * @throws IOException Error accessing the Lucene index.
	 */
	public void initialize(RetrievalModel r) throws IOException {

		Qry q = this.args.get(0);
		q.initialize(r);
	}

}
