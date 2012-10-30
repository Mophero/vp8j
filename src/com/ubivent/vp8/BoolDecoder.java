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

import java.io.DataInputStream;
import java.io.IOException;

public class BoolDecoder {
	private int bitcount;
	private DataInputStream input;
	private int range;
	private int value;

	public BoolDecoder(DataInputStream dis) throws IOException {
		value = dis.readUnsignedByte() << 8 | dis.readUnsignedByte();
		range = 255;
		this.input = dis;
		bitcount = 0;
	}

	public boolean readBool(int prob) throws IOException {
		int split = 1 + (((range - 1) * prob) >> 8);
		int SPLIT = split << 8;
		boolean retval; /* will be 0 or 1 */
		if (value >= SPLIT) { /* encoded a one */
			retval = true;
			range -= split; /* reduce range */
			value -= SPLIT; /* subtract off left endpoint of interval */
		} else { /* encoded a zero */
			retval = false;
			range = split; /* reduce range, no change in left endpoint */
		}
		while (range < 128) { /* shift out irrelevant value bits */
			value <<= 1;
			range <<= 1;
			if (++bitcount == 8) { /* shift in new bits 8 at a time */
				bitcount = 0;
				value |= input.readUnsignedByte();
			}
		}
		return retval;
	}

	public int readLiteral(int num_bits) throws IOException {
		int v = 0;
		while (num_bits-- > 0)
			v = (v << 1) + (readBool(128) ? 1 : 0);
		return v;
	}

	public boolean readBool() throws IOException {
		return readBool(128);
	}

	public int bool_maybe_get_int(int bits) throws IOException {
		return readBool(128) ? readLiteral(bits) : 0;
	}

	int bool_read_tree(final int t[], final char probs[]) throws IOException {
		int i = 0;

		while ((i = t[i + (readBool(probs[i >> 1]) ? 1 : 0)]) > 0)
			;

		return -i;
	}
}
