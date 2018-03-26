package src.comp557lw.a3;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.glfw.GLFW.*;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import mintools.parameters.BooleanParameter;
import mintools.parameters.IntParameter;
import mintools.swing.VerticalFlowPanel;
import mintools.viewer.EasyViewer;
import mintools.viewer.KeyCallback;
import mintools.viewer.SceneGraphNode;

/**
 * COMP557 - Assignment 4 class.
 */
public class A3App implements SceneGraphNode, KeyCallback {
	
    /**
     * @param args
     */
    public static void main(String[] args) {
        new A3App();
    }
    
    private PolygonSoup soup;
    
    private HEDS[] heds;
    
    private HalfEdge currentHE;
    
    private int whichSoup = 0;
    
    private String[] soupFiles = {            
            "meshdata/torus3.obj",  // all regular vertices, no boundary, all quads
            "meshdata/torus4.obj",
            "meshdata/torus6.obj",
            "meshdata/cylinder.obj",
            "meshdata/cube.obj",    // some irregular vertices, no boundary, all quads
            "meshdata/human.obj",         
            "meshdata/werewolf.obj", // no boundaries, mixed faces
            "meshdata/quad.obj",    // boundaries, all quads
            "meshdata/plane5x5.obj", 
            "meshdata/cutcube.obj",       
            "meshdata/monkey.obj",   // boundaries, mixed faces            
            "meshdata/350zrelease.obj",               
        };
    
    String keyboardInterfaceInstructions = 
    		"   space - half edge twin\n" +
    		"   n     - half edge next\n" +
    		"   left  - half edge child 1\n" +
    		"   right - half edge child 2\n" +
    		"   up    - half edge parent\n" +
    		"   m     - march half edge to the next quad\n" +
    		"\n" + 
    		"   home  - display mesh subdivision level - 1\n" +
    		"   end   - display subdivision level + 1\n" +
    		"\n" + 
    		"   page up   - previous model\n" +
    		"   page down - next model\n" +
    		"\n" + 
    		"   w     - toggle wireframe\n" +
    		"   s     - toggle draw soup\n" +
    		"   h     - toggle draw halfedge\n" +
    		"   v     - toggle draw child verts\n" +
    		"";
    
    /**
     * COMP557 - Assignment 3 class.
     */
    public A3App() {    
        loadSoupBuildAndSubdivide( soupFiles[0], 3 );
        EasyViewer ev = new EasyViewer("Comp 557 Assignment 4 - Alice Scott", this, new Dimension(600, 600), new Dimension(600, 600) );
        ev.setKeyCallback( this );
    }
    
    /**
     * Loads the currently 
     */
    private void loadSoupBuildAndSubdivide( String filename, int levels ) {          
        soup = new PolygonSoup( filename );
        heds = new HEDS[levels];
        heds[0] = new HEDS(soup);
        for ( int i = 1; i < levels; i++ ) {
            heds[i] = CatmullClark.subdivide( heds[i-1] );
        }
        if ( heds[0].faces.size() > 0 ) {
            // can only display a half edge and use the keyboard interface
            // to walk around the mesh if we actually have a half edge data
            // structure with faces!
            currentHE = heds[0].faces.get(0).he;
        }
        drawLevel = 0;        
    }

    @Override
    public void display() {
        if ( !drawWireFrame.getValue()) {
            // if drawing with lighting, we'll set the material
            // properties for the font and back surfaces, and set
            // polygons to render filled.
            final float frontColour[] = {.7f,.7f,0,1};
            final float backColour[] = {0,.7f,.7f,1};
            final float[] shinyColour = new float[] {1f, 1f, 1f, 1};            
            glEnable(GL_LIGHTING);
            glDisable( GL_CULL_FACE );
            glMaterialfv( GL_FRONT,GL_AMBIENT_AND_DIFFUSE, frontColour );
            glMaterialfv( GL_BACK,GL_AMBIENT_AND_DIFFUSE, backColour );
            glMaterialfv( GL_FRONT_AND_BACK,GL_SPECULAR, shinyColour );
            glMateriali( GL_FRONT_AND_BACK,GL_SHININESS, 50 );
            glLightModelf(GL_LIGHT_MODEL_TWO_SIDE, 1);
            glPolygonMode( GL_FRONT_AND_BACK, GL_FILL );            
        } else {
            // if drawing without lighting, we'll set the colour to white
            // and set polygons to render in wire frame
            glDisable( GL_LIGHTING );
            glColor4f(.7f,.7f,0.0f,1);
            glPolygonMode( GL_FRONT_AND_BACK, GL_LINE );
        }
        
        if ( drawSoup.getValue() ) {
            soup.display();
        }
        if ( drawChildVerts.getValue() ) {
            heds[drawLevel].drawChildVertices();
        }
        if ( drawHalfEdge.getValue() && currentHE != null ) {
            currentHE.display();
        }
        heds[drawLevel].display();
        
        EasyViewer.beginOverlay();
        glDisable( GL_LIGHTING );
        glColor3f(1,1,1);
        glLineWidth(1);
        EasyViewer.printTextLines( soupFiles[whichSoup] + "\nlevel " + drawLevel, 40, 40, 25 );
        EasyViewer.endOverlay();
    }
    
    @Override
    public void init() {
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        glEnable(GL_POINT_SMOOTH);
        glEnable(GL_NORMALIZE );
    }

    /** Level of subdivision of the mesh to draw */
    private int drawLevel = 0;
    
    private BooleanParameter drawHalfEdge = new BooleanParameter( "draw test half edge", true );
    private BooleanParameter drawChildVerts = new BooleanParameter( "draw child vertices", false );
    private BooleanParameter drawWireFrame = new BooleanParameter( "draw wire frame", false );
    // TODO: Objective 1: on the line below, set the drawCoarse default value to false once you've correctly created your half edge data structure
    private BooleanParameter drawSoup = new BooleanParameter( "draw coarse soup mesh", false );
    private IntParameter subdivisionLevels = new IntParameter("maximum subdivisions", 3, 3, Integer.MAX_VALUE );
    
    @Override
    public JPanel getControls() {
        VerticalFlowPanel vfp = new VerticalFlowPanel();
        vfp.add( drawWireFrame.getControls() );
        vfp.add( drawSoup.getControls() );        
        vfp.add( drawChildVerts.getControls() );
        vfp.add( drawHalfEdge.getControls() );
        vfp.add( subdivisionLevels.getControls() );
        vfp.add( new JLabel("increase maximum subdivision level carefully!" ) );        
        JButton reload = new JButton("Reload and resubdivide");
        vfp.add( reload );
        reload.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
            }
        });
        JTextArea ta = new JTextArea( keyboardInterfaceInstructions );        
        ta.setEditable(false);
        ta.setBorder( new TitledBorder("Keyboard controls") );
        vfp.add( ta );
        return vfp.getPanel();
    }
    
    public void invoke( long window, int key, int scancode, int action, int mods ) {
    	if ( action != GLFW_RELEASE ) return;
        if (key == GLFW_KEY_SPACE) {
        	if ( currentHE == null ) return;
            if ( currentHE.twin != null ) currentHE = currentHE.twin;                    
        } else if (key == GLFW_KEY_UP) {
        	if ( currentHE == null ) return;
            if ( currentHE.parent != null ) currentHE = currentHE.parent;
        } else if (key == GLFW_KEY_N) {
        	if ( currentHE == null ) return;
            if ( currentHE.next != null ) currentHE = currentHE.next;
        } else if (key == GLFW_KEY_LEFT) {
        	if ( currentHE == null ) return;
            if ( currentHE.child1 != null ) currentHE = currentHE.child1;                    
        } else if (key == GLFW_KEY_RIGHT) {
        	if ( currentHE == null ) return;
            if ( currentHE.child2 != null ) currentHE = currentHE.child2;
        } else if (key == GLFW_KEY_M) {
            // march along the mesh one quadrilateral at a time
        	if ( currentHE == null ) return;
            if ( currentHE.next != null ) currentHE = currentHE.next;
            if ( currentHE.next != null ) currentHE = currentHE.next;
            if ( currentHE.twin != null ) currentHE = currentHE.twin;
        } else if ( key == GLFW_KEY_PAGE_UP ) {
            if ( whichSoup > 0 ) whichSoup--;                    
            loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
        } else if ( key == GLFW_KEY_PAGE_DOWN ) {
            if ( whichSoup < soupFiles.length -1 ) whichSoup++;    
            loadSoupBuildAndSubdivide( soupFiles[whichSoup], subdivisionLevels.getValue() );
        } else if ( key == GLFW_KEY_HOME ) {
            drawLevel--;
            if ( drawLevel < 0 ) drawLevel = 0;
        } else if ( key == GLFW_KEY_END ) {
            drawLevel++;
            if ( drawLevel >= subdivisionLevels.getValue() ) drawLevel = subdivisionLevels.getValue() - 1;
        } else if ( key == GLFW_KEY_W ) {
        	drawWireFrame.setValue(!drawWireFrame.getValue());
        } else if ( key == GLFW_KEY_S ) {
            drawSoup.setValue(!drawSoup.getValue());
        } else if ( key == GLFW_KEY_V ) {
            drawChildVerts.setValue(!drawChildVerts.getValue());
        } else if ( key == GLFW_KEY_H ) {
            drawHalfEdge.setValue(!drawHalfEdge.getValue());
        }
    }

}
