package src.comp557lw.a3;

import static org.lwjgl.opengl.GL11.*;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Half edge data structure.
 * Maintains a list of faces (i.e., one half edge of each) to allow for easy display of geometry.
 * 
 * @author TODO: Alice Scott
 */
public class HEDS {

    /**
     * List of faces 
     */
    List<Face> faces = new ArrayList<Face>();
        
    /**
     * Constructs an empty mesh (used when building a mesh with subdivision)
     */
    public HEDS() {
        // do nothing
    }
        
    /**
     * Builds a half edge data structure from the polygon soup   
     * @param soup
     */
    public HEDS( PolygonSoup soup ) {
    	  // TODO: Objective 1: create the half edge data structure from a polygon soup
    	
    Map<String,HalfEdge> edge_map = new HashMap<String, HalfEdge>(); //stores a half-edge with its head vertex as key
    	
    	for (int[] f : soup.faceList) {
    		
    		HalfEdge prev = null;
    		for (int i = 0; i <= f.length-1; i++) {
    			
    			//find the edge (u,v) defining half-edge, where v is the head
    			// v is the current vertex at index i
    			// u is the previous vertex at index i-1 OR the last index if i=0
    			Vertex v = soup.vertexList.get(f[i]);
    			HalfEdge he = new HalfEdge();
    			he.head = v;
    	
    			Vertex u = new Vertex();
    			
    			if (i == 0) {
    				u = soup.vertexList.get(f[f.length-1]);
    			} else {
    				int j = i-1;
    				u = soup.vertexList.get(f[j]);
    			}
    			String uv_edge = u.toString() + v.toString();
    			edge_map.put(uv_edge,he);
    			 
    			
    			if (prev != null) {
    				prev.next = he;
    			} 
    			prev = he;
    			
    			// add in twins if already in list
      			String vu_edge = v.toString() + u.toString();
    			if(edge_map.containsKey(vu_edge)) {
    				HalfEdge twin = edge_map.get(vu_edge);
    				twin.twin = he;
    				he.twin = twin;
    			}
       		
    		}
    		String first_key = soup.vertexList.get(f[f.length-1]).toString() + soup.vertexList.get(f[0]).toString();
    		prev.next = edge_map.get(first_key);
    		Face face = new Face(edge_map.get(first_key));
    		faces.add(face);
    		}
    	
    	for (Face face : faces) {
    		HalfEdge curr = face.he;
    		do {
    			if (curr.head.n == null) {
    				setNormal(curr);
    			}
    			curr = curr.next;
    		} while (curr != face.he);
    	}
    } 
    
    public static void setNormal(HalfEdge h) {	
   	 
    	// loop through all the faces adjacent to a vertex
    	HalfEdge curr = h;
    	Vector3d normal = new Vector3d(0,0,0);
    	Vector3d t1 = new Vector3d();
    	Vector3d t2 = new Vector3d();
    	Double i = 0.0;
 	
    	int deg = 0;
    	do {
    		deg++;
    		curr = curr.next.twin;
    	}while(curr != h);
   	
    	curr = h;
    	do {
    		Vector3d p1 = new Vector3d(curr.twin.head.p);
    		p1.scale(2*Math.PI*i/deg);
            p1 = new Vector3d(Math.cos(p1.x), Math.cos(p1.y), Math.cos(p1.z));
            p1.normalize();
     		
    		Vector3d p2 = new Vector3d(curr.twin.head.p);
    		p2.scale(2*Math.PI*i/deg);
    		p2 = new Vector3d(Math.sin(p2.x), Math.sin(p2.y), Math.sin(p2.z));
    		p2.normalize();
    		
    		t1.add(p1);
    		t2.add(p2);
    		i++;
    		curr = curr.next.twin;
    	} while (curr != h);
    	
    	normal.cross(t1,t2);
    	normal.normalize();
    	h.head.n = normal;
    }
    

    

    /**
     * Draws the half edge data structure by drawing each of its faces.
     * Per vertex normals are used to draw the smooth surface when available,
     * otherwise a face normal is computed. 
     * @param drawable
     */
    public void display() {
        // note that we do not assume triangular or quad faces, so this method is slow! :(     
        Point3d p;
        Vector3d n;        
        for ( Face face : faces ) {
            HalfEdge he = face.he;
            if ( he.head.n == null ) { // don't have per vertex normals? use the face
                glBegin( GL_POLYGON );
                n = he.leftFace.n;
                glNormal3d( n.x, n.y, n.z );
                HalfEdge e = he;
                do {
                    p = e.head.p;
                    glVertex3d( p.x, p.y, p.z );
                    e = e.next;
                } while ( e != he );
                glEnd();
            } else {
                glBegin( GL_POLYGON );                
                HalfEdge e = he;
                do {
                    p = e.head.p;
                    n = e.head.n;
                    glNormal3d( n.x, n.y, n.z );
                    glVertex3d( p.x, p.y, p.z );
                    e = e.next;
                } while ( e != he );
                glEnd();
            }
        }
    }
    
    /** 
     * Draws all child vertices to help with debugging and evaluation.
     * (this will draw each points multiple times)
     * @param drawable
     */
    public void drawChildVertices() {
    	glDisable( GL_LIGHTING );
        glPointSize(8);
        glBegin( GL_POINTS );
        for ( Face face : faces ) {
            if ( face.child != null ) {
                Point3d p = face.child.p;
                glColor3f(0,0,1);
                glVertex3d( p.x, p.y, p.z );
            }
            HalfEdge loop = face.he;
            do {
                if ( loop.head.child != null ) {
                    Point3d p = loop.head.child.p;
                    glColor3f(1,0,0);
                    glVertex3d( p.x, p.y, p.z );
                }
                if ( loop.child1 != null && loop.child1.head != null ) {
                    Point3d p = loop.child1.head.p;
                    glColor3f(0,1,0);
                    glVertex3d( p.x, p.y, p.z );
                }
                loop = loop.next;
            } while ( loop != face.he );
        }
        glEnd();
        glEnable( GL_LIGHTING );
    }
}
