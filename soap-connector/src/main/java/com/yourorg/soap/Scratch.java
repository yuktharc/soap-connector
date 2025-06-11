@SpringBootTest
public class JAXBElementUtilsTest {

    @Test
    public void testWrapWithObjectFactory() {
        MyRequest req = new MyRequest();
        JAXBElement<MyRequest> wrapped = JAXBElementUtils.wrapWithJAXBElement(req);

        assertNotNull(wrapped);
        assertEquals(req, wrapped.getValue());
    }

    @Test
    public void testWrapWithFallbackQName() {
        DummyNoFactoryRequest req = new DummyNoFactoryRequest();
        JAXBElement<DummyNoFactoryRequest> wrapped = JAXBElementUtils.wrapWithJAXBElement(req);

        assertNotNull(wrapped);
        assertEquals("dummyNoFactoryRequest", wrapped.getName().getLocalPart());
        assertEquals(req, wrapped.getValue());
    }
}
