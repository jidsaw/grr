package geometry;

import org.junit.After;
import org.junit.Before;
import org.junit.jupiter.api.Test;

public class VectorTest {
    private Vector vec1;
    private Vector vec2;
    private Vector vec3;
    @Before
    public void init() {
        vec1 = new Vector();
        vec2 = new Vector();
        vec3 = new Vector();
    }
    @After
    public void tearDown() { vec1 = null; }
    @Test
    public void size(){

        if ((vec1.size()==4)){
            assertTrue(4);
        }
    }

}
