package me.leeho.filament.swt;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.google.android.filament.Box;
import com.google.android.filament.Camera;
import com.google.android.filament.Engine;
import com.google.android.filament.Engine.Backend;
import com.google.android.filament.EntityManager;
import com.google.android.filament.Filament;
import com.google.android.filament.IndexBuffer;
import com.google.android.filament.Material;
import com.google.android.filament.RenderableManager;
import com.google.android.filament.RenderableManager.PrimitiveType;
import com.google.android.filament.Renderer;
import com.google.android.filament.Scene;
import com.google.android.filament.Skybox;
import com.google.android.filament.VertexBuffer;
import com.google.android.filament.VertexBuffer.AttributeType;
import com.google.android.filament.VertexBuffer.VertexAttribute;
import com.google.android.filament.View;
import com.google.android.filament.Viewport;

import me.leeho.filament.FilamentTest;


/**
 *  just run this class for test
 * @author LeeHo
 *
 */
public class FilamentSWTTest {
	private Engine engine;
	private Renderer renderer;
	private Scene scene;
	private View view;
	private Camera camera;
	private VertexBuffer vertexBuffer;
	private IndexBuffer indexBuffer;
	private Material material;
	private FilamentSWTCanvas filamentSWTCanvas;

	public FilamentSWTTest(Canvas canvas) {
		this.filamentSWTCanvas = new FilamentSWTCanvas(canvas);
	}

	protected void init() {
		Filament.init();
		engine = Engine.create(Backend.OPENGL);
		renderer = engine.createRenderer();
		scene = engine.createScene();
		view = engine.createView();
		camera = engine.createCamera();
		initCanvas();
		setupView();
		setupScene();
	}

	private void initCanvas() {
		filamentSWTCanvas.getCanvas().addPaintListener(new PaintListener() {

			@Override
			public void paintControl(PaintEvent e) {
				paintCanvas(e);
			}
		});
		filamentSWTCanvas.getCanvas().addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				Point size = filamentSWTCanvas.getCanvas().getSize();
				onCanvasResize(size.x, size.y);
			}

			@Override
			public void controlMoved(ControlEvent e) {

			}
		});
		Point size = filamentSWTCanvas.getCanvas().getSize();
		onCanvasResize(size.x, size.y);
	}

	protected void paintCanvas(PaintEvent e) {
		System.out.println("paintCanvas");
		renderFrame();
	}

	private void loadMaterial() {
		InputStream resourceAsStream = FilamentTest.class.getResourceAsStream("baked_color.filamat");
		try {
			byte[] readAll = readAll(resourceAsStream);
			ByteBuffer byteBuffer = ByteBuffer.wrap(readAll);
			material = new Material.Builder().payload(byteBuffer, byteBuffer.remaining()).build(engine);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void createMesh() {
		int intSize = 4;
		int floatSize = 4;
		int shortSize = 2;
		// A vertex is a position + a color:
		// 3 floats for XYZ position, 1 integer for color
		int vertexSize = 3 * floatSize + intSize;

		// Define a vertex and a function to put a vertex in a ByteBuffer
		class Vertex {
			float x;
			float y;
			float z;
			int color;

			public Vertex(float x, float y, float z, int color) {
				super();
				this.x = x;
				this.y = y;
				this.z = z;
				this.color = color;
			}

			public void put(ByteBuffer buffer) {
				buffer.putFloat(x);
				buffer.putFloat(y);
				buffer.putFloat(z);
				buffer.putInt(color);
			}
		}

		// We are going to generate a single triangle
		int vertexCount = 3;
		double a1 = Math.PI * 2.0 / 3.0;
		double a2 = Math.PI * 4.0 / 3.0;

		ByteBuffer vertexData = ByteBuffer.allocate(vertexCount * vertexSize);

		// It is important to respect the native byte order
		vertexData.order(ByteOrder.nativeOrder());
		new Vertex(1.0f, 0.0f, 0.0f, 0xffff0000).put(vertexData);
		new Vertex((float) Math.cos(a1), (float) Math.sin(a1), 0.0f, 0xff00ff00).put(vertexData);
		new Vertex((float) Math.cos(a2), (float) Math.sin(a2), 0.0f, 0xff0000ff).put(vertexData);
		// Make sure the cursor is pointing in the right place in the byte buffer
		vertexData.flip();

		// Declare the layout of our mesh
		vertexBuffer = new VertexBuffer.Builder().bufferCount(1).vertexCount(vertexCount)
				// Because we interleave position and color data we must specify offset and
				// stride
				// We could use de-interleaved data by declaring two buffers and giving each
				// attribute a different buffer index
				.attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
				.attribute(VertexAttribute.COLOR, 0, AttributeType.UBYTE4, 3 * floatSize, vertexSize)
				// We store colors as unsigned bytes but since we want values between 0 and 1
				// in the material (shaders), we must mark the attribute as normalized
				.normalized(VertexAttribute.COLOR).build(engine);

		// Feed the vertex data to the mesh
		// We only set 1 buffer because the data is interleaved
		vertexBuffer.setBufferAt(engine, 0, vertexData);

		// Create the indices
		ByteBuffer indexData = ByteBuffer.allocate(vertexCount * shortSize).order(ByteOrder.nativeOrder())
				.putShort((short) 0).putShort((short) 1).putShort((short) 2);

		indexData.flip();

		indexBuffer = new IndexBuffer.Builder().indexCount(3).bufferType(IndexBuffer.Builder.IndexType.USHORT)
				.build(engine);

		indexBuffer.setBuffer(engine, indexData);
	}

	public static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		byte[] buf = new byte[512];
		int r;
		while ((r = in.read(buf)) != -1) {
			byteArrayOutputStream.write(buf, 0, r);
		}
		return byteArrayOutputStream.toByteArray();
	}

	private void setupScene() {
		loadMaterial();
		createMesh();

		// To create a renderable we first create a generic entity
		int renderable = EntityManager.get().create();

		// We then create a renderable component on that entity
		// A renderable is made of several primitives; in this case we declare only 1
		new RenderableManager.Builder(1)
				// Overall bounding box of the renderable
				.boundingBox(new Box(0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.01f))
				// Sets the mesh data of the first primitive
				.geometry(0, PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer, 0, 3)
				// Sets the material of the first primitive
				.material(0, material.getDefaultInstance()).build(engine, renderable);

		// Add the entity to the scene to render it
		scene.addEntity(renderable);
	}

	private void setupView() {

		scene.setSkybox(new Skybox.Builder().color(0.035f, 0.035f, 0.035f, 1.0f).build(engine));

		// NOTE: Try to disable post-processing (tone-mapping, etc.) to see the
		// difference
		// view.isPostProcessingEnabled = false

		// Tell the view which camera we want to use
		view.setCamera(camera);

		// Tell the view which scene we want to render
		view.setScene(scene);
	}

	private void renderFrame() {
		if (filamentSWTCanvas.beginFrame(engine, renderer)) {
			System.out.println("renderFrame");
			try {
				renderFrame0();
			} finally {
				filamentSWTCanvas.endFrame(renderer);
			}
		}
	}

	protected void onCanvasResize(int width, int height) {
		double zoom = 1.5;
		double aspect = 1.0 * width / height;
		camera.setProjection(Camera.Projection.ORTHO, -aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0);

		view.setViewport(new Viewport(0, 0, width, height));
		System.out.println("onCanvasResize:" + width + "/" + height);
	}

	private void renderFrame0() {
		renderer.render(view);
	}

	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("FilamentSWTTest");
		Canvas canvas = new Canvas(shell, SWT.None);
		canvas.setSize(640, 320);
		canvas.setLocation(200, 200);
		canvas.setBackground(new Color(100, 100, 100));
		shell.setLayout(new FillLayout());
		shell.open();
		FilamentSWTTest filamentSWTTest = new FilamentSWTTest(canvas);
		filamentSWTTest.init();
		while (!shell.isDisposed()) {
			display.readAndDispatch();
			if (canvas.isDisposed()) {
				break;
			}
			canvas.redraw();
			try {
				Thread.sleep(16);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		display.dispose();
	}
}
