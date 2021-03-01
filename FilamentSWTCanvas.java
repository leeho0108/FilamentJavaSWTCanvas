package me.leeho.filament.swt;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;

import com.google.android.filament.Engine;
import com.google.android.filament.FilamentPanel;
import com.google.android.filament.FilamentTarget;
import com.google.android.filament.Renderer;
import com.google.android.filament.SwapChain;

/**
 * 
 * modify from {@link FilamentPanel}
 * @author LeeHo
 *
 */
public class FilamentSWTCanvas implements FilamentTarget {
	private static final int SURFACE_PADDING = 0;
	private Control canvas;
	private Dimension mBackendDimension = new Dimension(0, 0);
	private NativeSurfaceEx mNativeSurface;
	private SwapChain mSwapChain;
	private int mWidth;
	private int mHeight;

	public FilamentSWTCanvas(Control canvas) {
		this.canvas = canvas;
	}

	public Control getCanvas() {
		return canvas;
	}

	/**
	 * This must be called on Filament thread. Prepare to renders to offscreen
	 * native window.
	 */
	public boolean beginFrame(Engine engine, Renderer renderer) {
		ensureSurface(engine);
		return renderer.beginFrame(mSwapChain, 0L);
	}

	/**
	 * This must be called on Filament thread. Pixels are read from the Gl (located
	 * in VRAM) and copied to the JVM (in RAM). Colors are converted from ABGR to
	 * RGBA. A BufferedImage is created (copy RAM to RAM of all pixels). Finally the
	 * image is drawn to screen via Graphics.drawImage (one more trip from RAM to
	 * VRAM).
	 */
	public void endFrame(Renderer renderer) {
		// By the time readPixel callback is invoked, the dimension of the Panel may
		// have changed.
		// Capture dimension so they match the current GL framebuffer.
		renderer.endFrame();
	}

	private void createSurfaces(Engine engine, int width, int height) {
		mBackendDimension = new Dimension(width, height);
		mNativeSurface = new NativeSurfaceEx(canvas.view.id, width, height);
		mSwapChain = engine.createSwapChainFromNativeSurface(mNativeSurface, 0L);
	}

	private void destroySurfaces(Engine engine) {

		if (mNativeSurface == null) {
			return;
		}

		// Previous frames using this surface may still be in the pipeline, so we must
		// wait for
		// them to finish.
		engine.flushAndWait();

		mNativeSurface.dispose();
		engine.destroySwapChain(mSwapChain);
	}

	private void ensureSurface(Engine engine) {
		// Capture the dimension at the time the surface was ensured.
		Point size = canvas.getSize();
		mWidth = size.x;
		mHeight = size.y;

		if (mBackendDimension.width < mWidth || mBackendDimension.height < mHeight) {
			destroySurfaces(engine);

			// A surface needs to be allocated. Allocate something a little bit bigger than
			// necessary in order to avoid reallocating too often if the Panel is resized.
			int widthToAllocate = mWidth + SURFACE_PADDING;
			int heightToAllocate = mHeight + SURFACE_PADDING;
			createSurfaces(engine, widthToAllocate, heightToAllocate);
		}
	}

	/**
	 * This must be called on Filament thread.
	 */
	public void destroy(Engine engine) {
		destroySurfaces(engine);
	}
}
