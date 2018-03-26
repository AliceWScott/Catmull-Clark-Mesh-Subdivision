package src.comp557lw.a3;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

/**
 * Class implementing the Catmull-Clark subdivision scheme
 * 
 * @author TODO: Alice Scott
 */
public class CatmullClark {

    /**
     * Subdivides the provided half edge data structure
     * @param heds
     * @return the subdivided mesh
     */
    public static HEDS subdivide( HEDS heds ) {
       
    	HEDS new_heds = new HEDS();
    	
        // TODO: Objectives 2,3,4: finish this method!!
        // you will certainly want to write lots of helper methods!
        
        find_even(heds);
        find_odd(heds);
        
        for(Face f: heds.faces) {
        	
        	// Loop through all the edges of the face
        	HalfEdge curr = f.he;
        	
        	
        	do {
        		
        		HalfEdge h1 = new HalfEdge();
        		HalfEdge h2 = new HalfEdge();
        		HalfEdge h3 = new HalfEdge();
        		HalfEdge h4 = new HalfEdge();
        		
        		// set the heads of new half-edges, or assign them to existing children
        		h1 = curr.child2; 
        		h2 = curr.next.child1;
        		h3.head = f.child;
        		h4.head = curr.child1.head;
        		
        		// set next half-edges for each
        		h1.next = h2;
        		h2.next = h3;
        		h3.next = h4;
        		h4.next = h1;
        		
        		h1.twin = curr.child2.twin;
        		h2.twin = curr.next.child1.twin;
        		
        		// if the twin of h3 and h4 already exist, point to them and make them point back
        		if (curr.next.child2.next != null && curr.next.child2.next.next.next != null) {
        			h3.twin = curr.next.child2.next.next.next;
        			curr.next.child2.next.next.next.twin = h3;
        		}
        		if ( curr.prev().child2.next != null && curr.prev().child2.next.next != null) {
        			h4.twin = curr.prev().child2.next.next;
        			curr.prev().child2.next.next.twin = h4;
        		}
        		
        		// point the half-edges to a newly created face
        		Face face = new Face(h1);
        		h1.leftFace = face;
        		h2.leftFace = face;
        		h3.leftFace = face;
        		h4.leftFace = face;	
        		new_heds.faces.add(face);
        		
        		curr = curr.next;
        	} while (curr != f.he);
        }
        
        for (Face f: new_heds.faces) {
        	HalfEdge curr = f.he;
        	do {
        		HEDS.setNormal(curr);
        		curr = curr.next;
        	}while(curr != f.he);
        }
        
        
        return new_heds;       
    }
    
 
   
    
    public static void find_even(HEDS heds) {
		
    	for (Face f: heds.faces) {
    		
    		HalfEdge curr = f.he;
    		// check if we have a quad face or other
    		int num_edges = 0; 
    		do {
    			num_edges++;
    			if (curr.next != null) {
    				curr = curr.next;
    			}
    		} while (curr != f.he);
    		
    		curr = f.he;
    		do {
    			
    			HalfEdge curr_copy = curr;
    			// find the degree of the half-edge's head vertex
        		int deg = 0;
        		do {
            		deg++;
            		if (curr_copy.next != null && curr_copy.next.twin != null) {
            			curr_copy = curr_copy.next.twin;
            		}
        		} while (curr_copy != curr);       		
    			
    			if (curr.head.child == null) {
    				Point3d new_point = new Point3d(0,0,0);
    				if (num_edges == 4) { // regular even subdivision
            			new_point.add(find_even(curr,deg));	
    				} else {
    					// Use special rule, we're dealing with an n-gon
    					new_point.add(find_even_special(curr,deg));
    				}
    				curr.head.child = new Vertex();
    				curr.head.child.p = new_point;	
    			}
    			curr = curr.next;
    		} while (curr != f.he);
    		
    	}
    }
    
public static Point3d find_even_special(HalfEdge h, int deg) {

	// Using an affine combo of new n-gon face vertices and original even vertex position.
	HalfEdge curr = h;
	Point3d p = new Point3d(0,0,0);
	int i = 0;
	do {
		p.add(get_face_point(curr.leftFace));
		i++;
		curr = curr.next.twin;
	} while (curr != h);
	p.add(h.head.p);
	p.scale(1/(i + 1.0));
	return p;
}
    
public static Point3d find_even(HalfEdge h, int deg) {
    	
    	HalfEdge curr = h;
		Point3d p = new Point3d(0,0,0);
		Point3d fp_avg = new Point3d(0,0,0);
		
		if (h.twin == null) { //boundary
			Point3d result = h.head.p;
			result.scale(3.0/4);
			Point3d prev = h.prev().head.p;
			prev.scale(1.0/8);			
			Point3d next = h.next.head.p;
			next.scale(1.0/8);
			result.add(prev);
			result.add(next);
			return result;
		}
		
		do {
			// add in all the surrounding edge midpoints
			Point3d p_update = get_edge_vertex(curr);
			p.add(p_update);
			
			// add in the surrounding face-points
			Point3d fp_update = get_face_point(curr.leftFace);
			fp_avg.add(fp_update);
			
			curr = curr.next.twin;
		} while (curr != h);

		//compute averages for the surrounding midpoints and face points
		p.scale(1.0/deg);
		fp_avg.scale(1.0/deg);
		
		// new vertex = (m_1 * old_vertex) +
		//				+ (m_2 * average of face points)
//						+ (m_3 * average of midpoint edge vertices)
		Double m_1 = (deg - 3.0)/ deg;
		Double m_2 = 1.0 / deg;
		Double m_3 = 2.0 / deg;
		
		Point3d result = h.head.p;
		result.scale(m_1);
		fp_avg.scale(m_2);
		p.scale(m_3);
		result.add(p);
		result.add(fp_avg);
		
    	return result;
    }

	public static Point3d get_face_point(Face f) {
		
		// calculate face point
		HalfEdge curr = f.he;
		Point3d face_point = new Point3d(0, 0, 0); //avg of all vertices of polygon face
		int num_edges = 0;
		
		do {
			num_edges++;
			face_point.add(curr.head.p);
			curr = curr.next;
		} while (curr != f.he);
		face_point.scale(1.0/num_edges);
		return face_point;
	}
	
	 // gets the average of the two face points 
    public static Point3d get_edge_vertex(HalfEdge h) {
    	
    	HalfEdge curr = h;
    	int num_edges = 0;
    	do {
    		num_edges++;
    		curr = curr.next;
    	} while (curr != h);
    	
    	Point3d edge_point = new Point3d(0,0,0);
    	
    	if (num_edges > 4) { // dealing with an n-gon
    		edge_point = get_face_point(h.leftFace);
    		Point3d twin_point =  get_face_point(h.twin.leftFace);
        	edge_point.add(twin_point);
        	edge_point.add(h.head.p);
        	edge_point.add(h.twin.head.p);
        	edge_point = new Point3d(edge_point.x / 4.0, edge_point.y / 4.0, edge_point.z / 4.0);
    	} else if (h.twin == null) { // boundary edge
    		edge_point = h.head.p;
    		edge_point.add(h.prev().head.p);
    		edge_point = new Point3d(edge_point.x / 2.0, edge_point.y / 2.0, edge_point.z / 2.0);
    	} else {
    		// we don't account for face points when dealing with quad mesh
    		edge_point = new Point3d(h.head.p.x * (3.0/8), h.head.p.y * (3.0/8), h.head.p.z * (3.0/8));
    		Point3d p = new Point3d(h.twin.head.p.x * (3.0/8), h.twin.head.p.y * (3.0/8), h.twin.head.p.z * (3.0/8));
    		edge_point.add(p);
    		p =  new Point3d(h.next.head.p.x * (1.0/16), h.next.head.p.y * (1.0/16), h.next.head.p.z * (1.0/16));
    		edge_point.add(p);
    		p =  new Point3d(h.next.next.head.p.x * (1.0/16), h.next.next.head.p.y * (1.0/16), h.next.next.head.p.z * (1.0/16));
    		edge_point.add(p);
    		p =  new Point3d(h.twin.next.head.p.x * (1.0/16), h.twin.next.head.p.y * (1.0/16), h.twin.next.head.p.z * (1.0/16));
    		edge_point.add(p);
    		p =  new Point3d(h.twin.next.next.head.p.x * (1.0/16), h.twin.next.next.head.p.y * (1.0/16), h.twin.next.next.head.p.z * (1.0/16));
    		edge_point.add(p);
    	}
    	
    	return edge_point;
    }
    
    public static void find_odd(HEDS heds) {

    	for (Face f: heds.faces) {
	
    		if (f.child == null) {
    			f.child = new Vertex();
    			f.child.p = get_face_point(f);
    		}
    		set_child_edges(f);
    	}
    }
    
    public static void set_child_edges(Face f) {
    	
    	//calculate edge points
		HalfEdge curr = f.he;

		do {
			Vertex v = new Vertex();
			v.p = get_edge_vertex(curr);
			
			// set children
			HalfEdge c1 = new HalfEdge();
			c1.head = v;
			curr.child1 = c1;
			c1.parent = curr;
			
			
			HalfEdge c2 = new HalfEdge();
			c2.head = curr.head.child;
			curr.child2 = c2;
			c2.parent = curr;
			
			HalfEdge twins_c1 = new HalfEdge();
			twins_c1.head = v;
			curr.twin.child1 = twins_c1;
			twins_c1.parent = curr.twin;
			
			HalfEdge twins_c2 = new HalfEdge();
			twins_c2.head = curr.twin.head.child;
			curr.twin.child2 = twins_c2;
			twins_c2.parent = curr.twin;
			
			
			// set child half-edge twins
			curr.child1.twin = curr.twin.child2;
			curr.child2.twin = curr.twin.child1;
			curr.twin.child1.twin = curr.child2;
			curr.twin.child2.twin = curr.child1;
			
			curr = curr.next;
		} while (curr != f.he);
		
    }
    
   
    
    
}
