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

public class Frame {
	public final static int VPX_PLANE_Y = 0;
	/** < Y (Luminance) plane */
	public final static int VPX_PLANE_U = 1;
	/** < U (Chroma) plane */
	public final static int VPX_PLANE_V = 2;
	/** < V (Chroma) plane */
	public final static int VPX_PLANE_ALPHA = 3;
	/** < A (Transparency) plane */

	int d_w;
	int d_h;
	byte img_data[];
	int w;
	int h;
	int stride[] = new int[3];
	int planes[] = new int[3];

	public Frame(int d_w, int d_h) {
		this.d_w = d_w;
		this.d_h = d_h;
		int bps = 12;
		int align = (1 << 1) - 1;
		int w = (d_w + align) & ~align;
		align = (1 << 1) - 1;
		int h = (d_h + align) & ~align;
		int s = bps * w / 8;
		int stride_align = 16;
		s = (s + stride_align - 1) & ~(stride_align - 1);
		img_data = new byte[h * s];
		this.w = w;
		this.h = h;

		/* Calculate strides */
		stride[VPX_PLANE_Y] = stride[VPX_PLANE_ALPHA] = s;
		stride[VPX_PLANE_U] = stride[VPX_PLANE_V] = s >> 1;

		/* Default viewport to entire image */
		vpx_img_set_rect(0, 0, d_w, d_h);
	}

	void vpx_img_set_rect(int x, int y, int w, int h) {

		if (x + w <= this.w && y + h <= this.h) {
			this.d_w = w;
			this.d_h = h;

			int offset = 0;
			planes[VPX_PLANE_Y] = x + y * stride[VPX_PLANE_Y];
			offset += this.h * stride[VPX_PLANE_Y];

			planes[VPX_PLANE_U] = offset + (x >> 1) + (y >> 1)
					* stride[VPX_PLANE_U];
			offset += (this.h >> 1) * stride[VPX_PLANE_U];
			planes[VPX_PLANE_V] = offset + (x >> 1) + (y >> 1)
					* stride[VPX_PLANE_V];
		}
	}
}
