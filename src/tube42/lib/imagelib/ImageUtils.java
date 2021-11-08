/*
 * This file is a part of the TUBE42 imagelib, released under the LGPL license.
 *
 * Development page: https://github.com/tube42/imagelib
 * License:          http://www.gnu.org/copyleft/lesser.html
 */

package tube42.lib.imagelib;


import javax.microedition.lcdui.*;


public final class ImageUtils
{
        
    
    // ---------------------------------------------
    
    /**
     * blend two images:
     */
    public static Image blend(Image img1, Image img2, int value256)
    {
        // 0. no change?
        if(value256 == 0xFF) return img1;
        if(value256 == 0x00) return img2;
        
        // 1. get blended image:
        int w1 = img1.getWidth();
        int h1 = img1.getHeight();
        int w2 = img2.getWidth();
        int h2 = img2.getHeight();
        
        int w0 = Math.min(w1, w2);
        int h0 = Math.min(h1, h2);        
        int [] data = new int[w0 * h0];
        int [] b1 = new int[w0];
        int [] b2 = new int[w0];
        
        value256 &= 0xFF;
        
        for(int offset = 0, i = 0; i < h0; i++) {
            img1.getRGB( b1, 0, w1, 0, i, w0, 1); // get one line from each
            img2.getRGB( b2, 0, w2, 0, i, w0, 1);
                        
            for(int j = 0; j < w0; j++)  // blend all pixels
                data[offset ++] = ColorUtils.blend( b1[j], b2[j], value256);
        }
        
        Image tmp = Image.createRGBImage(data, w0, h0, true);
        data = null; // can this help GC at this point?
        
        return tmp;
    }          
    
    /**
     * resize an image:
     */
    public static Image resize(Image src_i,
              int size_w, int size_h)
    {
                
        // set source size
        int w = src_i.getWidth();
        int h = src_i.getHeight();
        
        // no change??
        if(size_w == w && size_h == h) return src_i;
        
        int [] dst = new int[size_w * size_h];
        
        
           resize_rgb_filtered(src_i, dst, w, h, size_w, size_h);
        
        // not needed anymore
        src_i = null;
                                
        return Image.createRGBImage(dst, size_w, size_h, true);
    }
    
    private static final void resize_rgb_filtered(Image src_i, int [] dst, 
              int w0, int h0, int w1, int h1)
    {
        int [] buffer1 = new int[w0];
        int [] buffer2 = new int[w0];
        
        // UNOPTIMIZED bilinear filtering:               
        //         
        // The pixel position is defined by y_a and y_b,
        // which are 24.8 fixed point numbers
        // 
        // for bilinear interpolation, we use y_a1 <= y_a <= y_b1
        // and x_a1 <= x_a <= x_b1, with y_d and x_d defining how long
        // from x/y_b1 we are.
        //
        // since we are resizing one line at a time, we will at most 
        // need two lines from the source image (y_a1 and y_b1).
        // this will save us some memory but will make the algorithm 
        // noticeably slower
        
        for(int index1 = 0, y = 0; y < h1; y++) {
            
            final int y_a = ((y * h0) << 8) / h1;            
            final int y_a1 = y_a >> 8;            
            int y_d = y_a & 0xFF;
            
            int y_b1 = y_a1 + 1;            
            if(y_b1 >= h0) {
                y_b1 = h0-1;
                y_d = 0;
            }
            
            // get the two affected lines:
            src_i.getRGB(buffer1, 0, w0, 0, y_a1, w0, 1);            
            if(y_d != 0)
                src_i.getRGB(buffer2, 0, w0, 0, y_b1, w0, 1);
            
            for(int x = 0; x < w1; x++) {                 
                // get this and the next point
                int x_a = ((x * w0) << 8) / w1;
                int x_a1 = x_a >> 8;
                int x_d = x_a & 0xFF;
                
                
                int x_b1 = x_a1 + 1;                                
                if(x_b1 >= w0) {
                    x_b1 = w0-1;
                    x_d = 0;
                }
                
                
                // interpolate in x
                int c12, c34;
                int c1 = buffer1[x_a1];
                int c3 = buffer1[x_b1];
                
                // interpolate in y:
                if(y_d == 0) {   
                    c12 = c1;
                    c34 = c3;
                } else {
                    int c2 = buffer2[x_a1];
                    int c4 = buffer2[x_b1];
                    
                    c12 = ColorUtils.blend(c2, c1, y_d);
                    c34 = ColorUtils.blend(c4, c3, y_d);
                }
                
                // final result
                dst[index1++] = ColorUtils.blend(c34, c12, x_d);
            }
        }
        
    }
    
}
