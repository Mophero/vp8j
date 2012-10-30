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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import static com.ubivent.vp8.PredictionMode.*;

public class VP8Decoder {
	public final static int MAX_MB_SEGMENTS = 4;
	public final static int MB_FEATURE_TREE_PROBS = 3;
	public final static int FRAME_HEADER_SZ = 3;
	public final static int KEYFRAME_HEADER_SZ = 7;
	public final static int CURRENT_FRAME = 0;
	public final static int LAST_FRAME = 1;
	public final static int GOLDEN_FRAME = 2;
	public final static int ALTREF_FRAME = 3;
	public final static int NUM_REF_FRAMES = 4;

	int frameWidth = 0;
	int frameHeight = 0;
	int vertScale = 0;
	int horizScale = 0;
	boolean frameSizeUpdated = false;
	int partitions = 0;
	int partition_sz[];
	boolean is_keyframe = false;
	SegmentHeader segmentHeader = new SegmentHeader();
	LoopFilterHeader loopHeader = new LoopFilterHeader();
	QuantHeader quantHeader = new QuantHeader();
	ReferenceHeader referenceHeader = new ReferenceHeader();
	EntropyHeader entropyHeader = new EntropyHeader();
	EntropyHeader savedEntropy = new EntropyHeader();
	boolean savedEntropy_valid = false;
	Frame frame_strg[] = new Frame[NUM_REF_FRAMES];
	Frame ref_frames[] = new Frame[NUM_REF_FRAMES];
	DequantFactors dequant_factors[] = new DequantFactors[MAX_MB_SEGMENTS];
	int frame_cnt = 0;

	int mb_rows;
	int mb_cols;
	MB_Info mb_info_storage[];

	public VP8Decoder() {
		dequant_global_init();
	}

	private final boolean getBit(int n, int pos) {
		return (n & 1 << pos) != 0;
	}

	public void decodeFrame(byte buffer[]) throws IOException {
		DataInputStream dis = new DataInputStream(new ByteArrayInputStream(
				buffer));
		int tmp = dis.read();
		// common header
		is_keyframe = getBit(tmp, 7);

		if (is_keyframe)
			System.out.println("keyframe");
		else
			System.out.println("intraframe");

		int version = (tmp >> 4) & 7;
		assert (version < 4);
		System.out.println("version " + version);

		boolean showframe = getBit(tmp, 3);
		System.out.println("showframe " + showframe);

		int datalen = ((tmp & 7) << 16) | (dis.read() << 8) | dis.read();
		System.out.println("datalen " + datalen);

		BoolDecoder dec = null;

		if (is_keyframe) { // keyframe
			if (dis.read() != 0x9d || dis.read() != 0x01 || dis.read() != 0x2a) {
				System.out.println("wrong keyframe header");
				return;
			}
			int nextTwo = dis.read() << 8 | dis.read();
			frameSizeUpdated = false;
			tmp = nextTwo & 0x3fff;
			if (tmp != frameWidth)
				frameSizeUpdated = true;
			frameWidth = tmp;
			tmp = nextTwo >> 14;
			if (tmp != horizScale)
				frameSizeUpdated = true;
			horizScale = tmp;
			nextTwo = dis.read() << 8 | dis.read();
			tmp = nextTwo & 0x3fff;
			if (tmp != frameHeight)
				frameSizeUpdated = true;
			frameHeight = tmp;
			tmp = nextTwo >> 14;
			if (tmp != vertScale)
				frameSizeUpdated = true;
			vertScale = tmp;
			mb_cols = (frameWidth + 15) / 16;
			mb_rows = (frameHeight + 15) / 16;
			System.out.printf("w: %d %d h: %d %d\n", frameWidth, horizScale,
					frameHeight, vertScale);
		}
		dec = new BoolDecoder(dis);
		int colorSpace = dec.readLiteral(1);
		if (colorSpace > 0) {
			System.out.println("UNKNOWN COLORSPACE");
			return;
		}
		int clamp = dec.readLiteral(1);
		assert (clamp == 0);
		decode_segmentation_header(dec, is_keyframe);
		decode_loopfilter_header(dec, is_keyframe);
		decode_and_init_token_partitions(dec, buffer, datalen);
		decode_quantizer_header(dec);
		decode_reference_header(dec);
		/*
		 * Set keyframe entropy defaults. These get updated on keyframes
		 * regardless of the refresh_entropy setting.
		 */
		if (is_keyframe) {
			EntropyHeader.copyArray4(entropyHeader.coeff_probs,
					ProbData.k_default_coeff_probs);
			EntropyHeader.copyArray2(entropyHeader.mv_probs,
					ProbData.k_default_mv_probs);
			EntropyHeader.copyArray1(entropyHeader.y_mode_probs,
					ProbData.k_default_y_mode_probs);
			EntropyHeader.copyArray1(entropyHeader.uv_mode_probs,
					ProbData.k_default_uv_mode_probs);
		}

		if (!referenceHeader.refresh_entropy) {
			savedEntropy = (EntropyHeader) entropyHeader.clone();
			savedEntropy_valid = true;
		}
		decode_entropy_header(dec);
		vp8_dixie_modemv_init();
		vp8_dixie_tokens_init();
		vp8_dixie_predict_init();
		dequant_init();
		int row = 0;
		int partition = 0;
		for (row = 0, partition = 0; row < mb_rows; row++) {
			vp8_dixie_modemv_process_row(dec, row, 0, mb_cols);
			vp8_dixie_tokens_process_row(partition, row, 0, mb_cols);
			vp8_dixie_predict_process_row(row, 0, mb_cols);

			if (loopHeader.level != 0 && row != 0)
				vp8_dixie_loopfilter_process_row(row - 1, 0, mb_cols);

			if (++partition == partitions)
				partition = 0;
		}

		if (loopHeader.level != 0)
			vp8_dixie_loopfilter_process_row(row - 1, 0, mb_cols);

		frame_cnt++;

		if (referenceHeader.refresh_entropy) {
			entropyHeader = (EntropyHeader) savedEntropy.clone();
			savedEntropy_valid = false;
		}

		/* Handle reference frame updates */
		if (referenceHeader.copy_arf == 1) {
			ref_frames[ALTREF_FRAME] = ref_frames[LAST_FRAME];
		} else if (referenceHeader.copy_arf == 2) {
			ref_frames[ALTREF_FRAME] = ref_frames[GOLDEN_FRAME];
		}

		if (referenceHeader.copy_gf == 1) {
			ref_frames[GOLDEN_FRAME] = ref_frames[LAST_FRAME];
		}

		else if (referenceHeader.copy_gf == 2) {
			ref_frames[GOLDEN_FRAME] = ref_frames[ALTREF_FRAME];
		}

		if (referenceHeader.refresh_gf) {
			ref_frames[GOLDEN_FRAME] = ref_frames[CURRENT_FRAME];
		}

		if (referenceHeader.refresh_arf) {
			ref_frames[ALTREF_FRAME] = ref_frames[CURRENT_FRAME];
		}

		if (referenceHeader.refresh_last) {
			ref_frames[LAST_FRAME] = ref_frames[CURRENT_FRAME];
		}
	}

	 void
	   vp8_dixie_loopfilter_process_row(
	                                     int            row,
	                                     int            start_col,
	                                     int            num_cols)
	   {
	       if (loopHeader.use_simple)
	           filter_row_simple(row, start_col, num_cols);
	       else
	           filter_row_normal(row, start_col, num_cols);
	   }
	
	
	   void
	   vp8_dixie_tokens_process_row(
	                                 int            partition,
	                                 int            row,
	                                 int            start_col,
	                                 int            num_cols)
	   {
	       token_decoder tokens = this.tokens[partition];
	       short              coeffs = tokens.coeffs + 25 * 16 * start_col;
	        int       col;
	       token_entropy_ctx_t  *above = ctx->above_token_entropy_ctx
	                                     + start_col;
	       token_entropy_ctx_t  *left = &tokens->left_token_entropy_ctx;
	       int       mbi = (row+1)*mbi_w + start_col;


	       if (row == 0)
	           reset_above_context(above, num_cols);

	       if (start_col == 0)
	           reset_row_context(left);

	       for (col = start_col; col < start_col + num_cols; col++)
	       {
	           memset(coeffs, 0, 25 * 16 * sizeof(short));

	           if (mbi->base.skip_coeff)
	           {
	               reset_mb_context(left, above, mbi->base.y_mode);
	               mbi->base.eob_mask = 0;
	           }
	           else
	           {
	               struct dequant_factors *dqf;

	               dqf = ctx->dequant_factors  + mbi->base.segment_id;
	               mbi->base.eob_mask =
	                   decode_mb_tokens(&tokens->bool,
	                                    *left, *above,
	                                    coeffs,
	                                    mbi->base.y_mode,
	                                    ctx->entropy_hdr.coeff_probs,
	                                    dqf->factor);
	           }

	           above++;
	           mbi++;
	           coeffs += 25 * 16;
	       }
	   }
	
	
	int mbi_w, mbi_h;
	
	private void vp8_dixie_modemv_init() {

		mbi_w = mb_cols + 1; /* For left border col */
		mbi_h = mb_rows + 1; /* For above border row */

		if (frameSizeUpdated) {
			mb_info_storage = null;
		}

		if (mb_info_storage == null)
			mb_info_storage = new MB_Info[mbi_w * mbi_h];
		for(int i = 0; i < mbi_w * mbi_h; i++) {
			mb_info_storage[i] = new MB_Info();
		}
	}
	
	    byte
	   read_segment_id(BoolDecoder bool) throws IOException
	   {
	       return (byte) (bool.readBool(segmentHeader.tree_probs[0])
	              ? 2 + (bool.readBool(segmentHeader.tree_probs[2])?1:0)
	              : (bool.readBool(segmentHeader.tree_probs[1])?1:0));
	   }
	
	   void
	   vp8_dixie_modemv_process_row(BoolDecoder bool,
	   int                     row,
	   int                     start_col,
	   int                     num_cols) throws IOException
	   {
	       int       above;
	       int current;
	        int          col;
	        mv_clamp_rect  bounds = new mv_clamp_rect();

	       current = mbi_w*(row+1) + start_col;
	       above = mbi_w*row + start_col;

	       /* Calculate the eighth-pel MV bounds using a 1 MB border. */
	       bounds.to_left   = -((start_col + 1) << 7);
	       bounds.to_right  = (mb_cols - start_col) << 7;
	       bounds.to_top    = -((row + 1) << 7);
	       bounds.to_bottom = (mb_rows - row) << 7;

	       for (col = start_col; col < start_col + num_cols; col++)
	       {
	           if (segmentHeader.update_map)
	        	   mb_info_storage[current].segment_id = read_segment_id(bool);

	           if (entropyHeader.coeff_skip_enabled)
	        	   mb_info_storage[current].skip_coeff = bool.readBool(entropyHeader.coeff_skip_prob);

	           if (is_keyframe)
	           {
	               if (!segmentHeader.update_map)
	            	   mb_info_storage[current].segment_id = 0;

	               decode_kf_mb_mode(current, current - 1, above, bool);
	           } /* TODO do keyframes first
	           else
	           {
	               if (bool_get(bool, ctx->entropy_hdr.prob_inter))
	                   decode_mvs(ctx, this, this - 1, above, &bounds,
	                              bool);
	               else
	                   decode_intra_mb_mode(this, &ctx->entropy_hdr, bool);

	               bounds.to_left -= 16 << 3;
	               bounds.to_right -= 16 << 3;
	           } */

	           /* Advance to next mb */
	           current++;
	           above++;
	       }
	   }

	   byte
	   left_block_mode(int current,
	                   int left,
	                   int b)
	   {
	       if ((b & 3) > 0)
	       {
	           switch (mb_info_storage[left].y_mode)
	           {
	           case DC_PRED:
	               return B_DC_PRED;
	           case V_PRED:
	               return B_VE_PRED;
	           case H_PRED:
	               return B_HE_PRED;
	           case TM_PRED:
	               return B_TM_PRED;
	           case B_PRED:
	               return mb_info_storage[left].modes[b+3];
	           default:
	               assert(false);
	           }
	       }

	       return mb_info_storage[current].modes[b-1];
	   }
	   
	   byte
	   above_block_mode(int current,
	                    int above,
	                    int b)
	   {
	       if (b < 4)
	       {
	           switch (mb_info_storage[above].y_mode)
	           {
	           case DC_PRED:
	               return B_DC_PRED;
	           case V_PRED:
	               return B_VE_PRED;
	           case H_PRED:
	               return B_HE_PRED;
	           case TM_PRED:
	               return B_TM_PRED;
	           case B_PRED:
	               return mb_info_storage[above].modes[b+12];
	           default:
	               assert(false); // OOPS
	           }
	       }

	       return mb_info_storage[current].modes[b-4];
	   }
	   
	   
	   void
	   decode_kf_mb_mode(int current, int left, int above, BoolDecoder bool) throws IOException
	   {
	       int y_mode, uv_mode;

	       y_mode = bool.bool_read_tree(ModevData.kf_y_mode_tree, ModevData.kf_y_mode_probs);

	       if (y_mode == PredictionMode.B_PRED)
	       {
	            int i;

	           for (i = 0; i < 16; i++)
	           {
	               byte a = above_block_mode(current, above,
	                                                         i);
	               byte l = left_block_mode(current, left, i);
	               byte b;

	               b = (byte) bool.bool_read_tree(ModevData.b_mode_tree,
	                                  ModevData.kf_b_mode_probs[a][l]);
	               mb_info_storage[current].modes[i] = b;
	           }
	       }

	       uv_mode = bool.bool_read_tree(ModevData.uv_mode_tree, ModevData.kf_uv_mode_probs);

	       mb_info_storage[current].y_mode = (byte) y_mode;
	       mb_info_storage[current].uv_mode = (byte) uv_mode;
	       mb_info_storage[current].mv.x = mb_info_storage[current].mv.y = 0;
	       mb_info_storage[current].ref_frame = 0;
	   }
	
	class token_decoder {
		BoolDecoder bool;
		int left_token_entropy_ctx[] = new int[4 + 2 + 2 + 1];
		short coeffs[];

	}

	void vp8_dixie_predict_init() {

		int i;

		if (frameSizeUpdated) {
			for (i = 0; i < NUM_REF_FRAMES; i++) {
				int w = mb_cols * 16 + BORDER_PIXELS * 2;
				int h = mb_rows * 16 + BORDER_PIXELS * 2;

				frame_strg[i] = new Frame(w, h);

				frame_strg[i].vpx_img_set_rect(BORDER_PIXELS, BORDER_PIXELS,
						frameWidth, frameHeight);

			}
			/*
			 * TODO if (ctx->frame_hdr.version) ctx->subpixel_filters =
			 * bilinear_filters; else ctx->subpixel_filters = sixtap_filters;
			 */
		}

		/* Find a free framebuffer to predict into */
		ref_frames[CURRENT_FRAME] = null;

		ref_frames[CURRENT_FRAME] = vp8_dixie_find_free_ref_frame();
	}

	Frame vp8_dixie_find_free_ref_frame() {
		for (Frame f : frame_strg) {
			boolean found = false;
			for (Frame f2 : ref_frames) {
				if (f2 == f)
					found = true;
			}
			if (!found)
				return f;
		}
		System.out.println("NO FREE FRAME");
		return null;
	}

	private static int clamp_q(int q) {
		if (q < 0)
			return 0;
		else if (q > 127)
			return 127;

		return q;
	}

	private static int dc_q(int q) {
		return DequantData.dc_q_lookup[clamp_q(q)];
	}

	private static int ac_q(int q) {
		return DequantData.ac_q_lookup[clamp_q(q)];
	}

	private void dequant_init() {
		int i, q;
		int factorsOffset = 0;
		DequantFactors dqf = dequant_factors[factorsOffset++];
		for (i = 0; i < (segmentHeader.enabled ? MAX_MB_SEGMENTS : 1); i++) {
			q = quantHeader.q_index;
			if (segmentHeader.enabled)
				q = (!segmentHeader.abs) ? q + segmentHeader.quant_idx[i]
						: segmentHeader.quant_idx[i];

			if (dqf.quant_idx != q || quantHeader.delta_update != 0) {
				dqf.factor[DequantFactors.TOKEN_BLOCK_Y1][0] = (short) dc_q(q
						+ quantHeader.y1_dc_delta_q);
				dqf.factor[DequantFactors.TOKEN_BLOCK_Y1][1] = (short) ac_q(q);
				dqf.factor[DequantFactors.TOKEN_BLOCK_UV][0] = (short) dc_q(q
						+ quantHeader.uv_dc_delta_q);
				dqf.factor[DequantFactors.TOKEN_BLOCK_UV][1] = (short) ac_q(q
						+ quantHeader.uv_ac_delta_q);
				dqf.factor[DequantFactors.TOKEN_BLOCK_Y2][0] = (short) (dc_q(q
						+ quantHeader.y2_dc_delta_q) * 2);
				dqf.factor[DequantFactors.TOKEN_BLOCK_Y2][1] = (short) (ac_q(q
						+ quantHeader.y2_ac_delta_q) * 155 / 100);

				if (dqf.factor[DequantFactors.TOKEN_BLOCK_Y2][1] < 8)
					dqf.factor[DequantFactors.TOKEN_BLOCK_Y2][1] = 8;

				if (dqf.factor[DequantFactors.TOKEN_BLOCK_UV][0] > 132)
					dqf.factor[DequantFactors.TOKEN_BLOCK_UV][0] = 132;

				dqf.quant_idx = q;
			}

			dqf = dequant_factors[factorsOffset++];
		}
	}

	public static final int MAX_PARTITIONS = 8;
	public static final int BORDER_PIXELS = 16;
	int above_token_entropy_ctx[] = new int[4 + 2 + 2 + 1];
	token_decoder tokens[] = new token_decoder[MAX_PARTITIONS];

	private void vp8_dixie_tokens_init() {
		int partitions = this.partitions;

		if (frameSizeUpdated) {
			int i;
			int coeff_row_sz = mb_cols * 25 * 16;

			for (i = 0; i < partitions; i++) {

				tokens[i].coeffs = new short[coeff_row_sz];

			}
		}
	}

	private void decode_entropy_header(BoolDecoder bool) throws IOException {
		int i, j, k, l;

		/* Read coefficient probability updates */
		for (i = 0; i < ProbData.BLOCK_TYPES; i++)
			for (j = 0; j < ProbData.COEFF_BANDS; j++)
				for (k = 0; k < ProbData.PREV_COEFF_CONTEXTS; k++)
					for (l = 0; l < ProbData.ENTROPY_NODES; l++)
						if (bool.readBool(ProbData.k_coeff_entropy_update_probs[i][j][k][l]))
							entropyHeader.coeff_probs[i][j][k][l] = (short) bool
									.readLiteral(8);

		/* Read coefficient skip mode probability */
		entropyHeader.coeff_skip_enabled = bool.readBool();

		if (entropyHeader.coeff_skip_enabled)
			entropyHeader.coeff_skip_prob = (short) bool.readLiteral(8);

		/* Parse interframe probability updates */
		if (!is_keyframe) {
			entropyHeader.prob_inter = (short) bool.readLiteral(8);
			entropyHeader.prob_last = (short) bool.readLiteral(8);
			entropyHeader.prob_gf = (short) bool.readLiteral(8);

			if (bool.readBool())
				for (i = 0; i < 4; i++)
					entropyHeader.y_mode_probs[i] = (short) bool.readLiteral(8);

			if (bool.readBool())
				for (i = 0; i < 3; i++)
					entropyHeader.uv_mode_probs[i] = (short) bool
							.readLiteral(8);

			for (i = 0; i < 2; i++)
				for (j = 0; j < ProbData.MV_PROB_CNT; j++)
					if (bool.readBool(ProbData.k_mv_entropy_update_probs[i][j])) {
						int x = (short) bool.readLiteral(7);
						entropyHeader.mv_probs[i][j] = (short) ((x != 0) ? (x << 1)
								: 1);
					}
		}
	}

	private void decode_reference_header(BoolDecoder bool) throws IOException {
		referenceHeader.refresh_gf = is_keyframe ? true : bool.readBool();
		referenceHeader.refresh_arf = is_keyframe ? true : bool.readBool();
		referenceHeader.copy_gf = is_keyframe ? 0
				: !referenceHeader.refresh_gf ? bool.readLiteral(2) : 0;
		referenceHeader.copy_arf = is_keyframe ? 0
				: !referenceHeader.refresh_arf ? bool.readLiteral(2) : 0;
		referenceHeader.sign_bias[GOLDEN_FRAME] = is_keyframe ? false : bool
				.readBool();
		referenceHeader.sign_bias[ALTREF_FRAME] = is_keyframe ? false : bool
				.readBool();
		referenceHeader.refresh_entropy = bool.readBool();
		referenceHeader.refresh_last = is_keyframe ? true : bool.readBool();
	}

	void decode_quantizer_header(BoolDecoder bool) throws IOException {
		int update;
		int last_q = quantHeader.q_index;

		quantHeader.q_index = bool.readLiteral(7);
		update = (last_q != quantHeader.q_index) ? 1 : 0;
		update |= (quantHeader.y1_dc_delta_q = bool.bool_maybe_get_int(4));
		update |= (quantHeader.y2_dc_delta_q = bool.bool_maybe_get_int(4));
		update |= (quantHeader.y2_ac_delta_q = bool.bool_maybe_get_int(4));
		update |= (quantHeader.uv_dc_delta_q = bool.bool_maybe_get_int(4));
		update |= (quantHeader.uv_ac_delta_q = bool.bool_maybe_get_int(4));
		quantHeader.delta_update = update;
	}

	private void decode_segmentation_header(BoolDecoder bool, boolean isKeyframe)
			throws IOException {
		if (isKeyframe)
			segmentHeader = new SegmentHeader();

		segmentHeader.enabled = bool.readBool();

		if (segmentHeader.enabled) {
			int i;

			segmentHeader.update_map = bool.readBool();
			segmentHeader.update_data = bool.readBool();

			if (segmentHeader.update_data) {
				segmentHeader.abs = bool.readBool();

				for (i = 0; i < MAX_MB_SEGMENTS; i++)
					segmentHeader.quant_idx[i] = bool.bool_maybe_get_int(7);

				for (i = 0; i < MAX_MB_SEGMENTS; i++)
					segmentHeader.lf_level[i] = bool.bool_maybe_get_int(6);
			}

			if (segmentHeader.update_map) {
				for (i = 0; i < MB_FEATURE_TREE_PROBS; i++)
					segmentHeader.tree_probs[i] = bool.readBool() ? bool
							.readLiteral(8) : 255;
			}
		} else {
			segmentHeader.update_map = false;
			segmentHeader.update_data = false;
		}
	}

	private void decode_loopfilter_header(BoolDecoder bool, boolean isKeyframe)
			throws IOException {
		if (isKeyframe)
			loopHeader = new LoopFilterHeader();

		loopHeader.use_simple = bool.readBool();
		loopHeader.level = bool.readLiteral(6);
		loopHeader.sharpness = bool.readLiteral(3);
		loopHeader.delta_enabled = bool.readBool();

		if (loopHeader.delta_enabled && bool.readBool()) {
			int i;

			for (i = 0; i < LoopFilterHeader.BLOCK_CONTEXTS; i++)
				loopHeader.ref_delta[i] = bool.bool_maybe_get_int(6);

			for (i = 0; i < LoopFilterHeader.BLOCK_CONTEXTS; i++)
				loopHeader.mode_delta[i] = bool.bool_maybe_get_int(6);
		}
	}

	private void decode_and_init_token_partitions(BoolDecoder bool,
			byte buffer[], int datalen) throws IOException {
		int i;

		partitions = 1 << bool.readLiteral(2);
		int sz = buffer.length;
		int dataoffset = FRAME_HEADER_SZ
				+ (is_keyframe ? KEYFRAME_HEADER_SZ : 0) + datalen;
		sz -= buffer.length - dataoffset;

		sz -= 3 * (partitions - 1);
		partition_sz = new int[partitions];

		for (i = 0; i < partitions; i++) {
			if (i < partitions - 1) {
				partition_sz[i] = (buffer[2 + dataoffset] << 16)
						| (buffer[1 + dataoffset] << 8) | buffer[dataoffset];
				dataoffset += 3;
			} else
				partition_sz[i] = sz;

			if (sz < partition_sz[i])
				throw new IOException("ERROR part size");

			sz -= partition_sz[i];
		}

		for (i = 0; i < partitions; i++) {
			tokens[i].bool = new BoolDecoder(new DataInputStream(
					new ByteArrayInputStream(buffer, dataoffset,
							partition_sz[i])));
			dataoffset += partition_sz[i];
		}
	}

	void dequant_global_init() {
		int i;

		for (i = 0; i < MAX_MB_SEGMENTS; i++) {
			dequant_factors[i] = new DequantFactors();
			dequant_factors[i].quant_idx = -1;
		}
	}

	
	
	public void getFrame(ByteBuffer buffer) {
		return;
	}
}
