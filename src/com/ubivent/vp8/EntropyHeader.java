 /*
    * Copyright (c) 2012, ubivent GmbH, Thomas Butter, Oliver Seuffert
    * All right reserved.
    * 
    * Use of this source code is governed by a BSD-style license
    * that can be found in the LICENSE file in the root of the source
    * tree.
    * 
    * This software is based on RFC6386
    * 
    * Copyright (c) 2010, 2011, Google Inc.  All rights reserved.
    *
    * Use of this source code is governed by a BSD-style license
    * that can be found in the LICENSE file in the root of the source
    * tree.  An additional intellectual property rights grant can be
    * found in the file PATENTS.  All contributing project authors may
    * be found in the AUTHORS file in the root of the source tree.
    */

package com.ubivent.vp8;

public class EntropyHeader {

	short coeff_probs[][][][] = new short[ProbData.BLOCK_TYPES][ProbData.COEFF_BANDS][ProbData.PREV_COEFF_CONTEXTS][ProbData.ENTROPY_NODES];
	short mv_probs[][] = new short[2][ProbData.MV_PROB_CNT];
	boolean coeff_skip_enabled;
	short coeff_skip_prob;
	short y_mode_probs[] = new short[4];
	short uv_mode_probs[] = new short[3];
	short prob_inter;
	short prob_last;
	short prob_gf;

	
	
	@Override
	protected Object clone() {
		EntropyHeader res = new EntropyHeader();
		copyArray4(coeff_probs, res.coeff_probs);
		copyArray2(mv_probs, res.mv_probs);
		res.coeff_skip_enabled = coeff_skip_enabled;
		res.coeff_skip_prob = coeff_skip_prob;
		copyArray1(y_mode_probs, res.y_mode_probs);
		copyArray1(uv_mode_probs, res.uv_mode_probs);
		res.prob_inter = prob_inter;
		res.prob_last = prob_last;
		res.prob_gf = prob_gf;
		return res;
	}

	static void copyArray1(short src[], short dst[]) {
		for (int i1 = 0; i1 < src.length; i1++) {
			src[i1] = dst[i1];
		}
	}

	static void copyArray2(short src[][], short dst[][]) {
		for (int i1 = 0; i1 < src.length; i1++) {
			for (int i2 = 0; i2 < src[i1].length; i2++) {
				src[i1][i2] = dst[i1][i2];
			}
		}
	}

	static void copyArray4(short[][][][] src, short[][][][] dst) {
		for (int i1 = 0; i1 < src.length; i1++) {
			for (int i2 = 0; i2 < src[i1].length; i2++) {
				for (int i3 = 0; i3 < src[i2].length; i3++) {
					for (int i4 = 0; i4 < src[i3].length; i4++) {
						src[i1][i2][i3][i4] = dst[i1][i2][i3][i4];
					}
				}
			}
		}
	}
}
