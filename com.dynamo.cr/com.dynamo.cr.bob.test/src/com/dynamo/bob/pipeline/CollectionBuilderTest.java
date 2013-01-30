package com.dynamo.bob.pipeline;

import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import javax.vecmath.AxisAngle4d;
import javax.vecmath.Point3d;
import javax.vecmath.Quat4d;
import javax.vecmath.Vector3d;

import org.junit.Assert;
import org.junit.Test;

import com.dynamo.bob.Builder;
import com.dynamo.bob.CompileExceptionError;
import com.dynamo.bob.pipeline.ProtoBuilders.CollectionBuilder;
import com.dynamo.bob.test.util.PropertiesTestUtil;
import com.dynamo.bob.util.MurmurHash;
import com.dynamo.gameobject.proto.GameObject.CollectionDesc;
import com.dynamo.gameobject.proto.GameObject.ComponentPropertyDesc;
import com.dynamo.gameobject.proto.GameObject.InstanceDesc;
import com.dynamo.properties.proto.PropertiesProto.PropertyDeclarations;
import com.dynamo.proto.DdfMath.Point3;
import com.dynamo.proto.DdfMath.Quat;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

public class CollectionBuilderTest extends AbstractProtoBuilderTest {

    private static final double epsilon = 0.000001;

    @Override
    protected Builder<Void> createBuilder() {
        return new CollectionBuilder();
    }

    @Override
    protected Message parseMessage(byte[] content) throws InvalidProtocolBufferException {
        return CollectionDesc.parseFrom(content);
    }

    @Test
    public void testProps() throws Exception {
        addFile("/test.go", "");
        StringBuilder src = new StringBuilder();
        src.append("name: \"main\"\n");
        src.append("instances {\n");
        src.append("  id: \"test\"\n");
        src.append("  prototype: \"/test.go\"\n");
        src.append("  component_properties {\n");
        src.append("    id: \"test\"\n");
        src.append("    properties { id: \"number\" value: \"1\" type: PROPERTY_TYPE_NUMBER }\n");
        src.append("    properties { id: \"hash\" value: \"hash\" type: PROPERTY_TYPE_HASH }\n");
        src.append("    properties { id: \"url\" value: \"url\" type: PROPERTY_TYPE_URL }\n");
        src.append("    properties { id: \"vec3\" value: \"1, 2, 3\" type: PROPERTY_TYPE_VECTOR3 }\n");
        src.append("    properties { id: \"vec4\" value: \"4, 5, 6, 7\" type: PROPERTY_TYPE_VECTOR4 }\n");
        src.append("    properties { id: \"quat\" value: \"8, 9, 10, 11\" type: PROPERTY_TYPE_QUAT }\n");
        src.append("  }\n");
        src.append("}\n");
        CollectionDesc collection = (CollectionDesc)build("/test.collection", src.toString());
        for (InstanceDesc instance : collection.getInstancesList()) {
            for (ComponentPropertyDesc compProp : instance.getComponentPropertiesList()) {
                PropertyDeclarations properties = compProp.getPropertyDecls();
                PropertiesTestUtil.assertNumber(properties, 1, 0);
                PropertiesTestUtil.assertHash(properties, MurmurHash.hash64("hash"), 0);
                PropertiesTestUtil.assertURL(properties, "url", 0);
                PropertiesTestUtil.assertVector3(properties, 1, 2, 3, 0);
                PropertiesTestUtil.assertVector4(properties, 4, 5, 6, 7, 0);
                PropertiesTestUtil.assertQuat(properties, 8, 9, 10, 11, 0);
            }
        }
    }

    @Test(expected = CompileExceptionError.class)
    public void testPropInvalidValue() throws Exception {
        addFile("/test.go", "");
        StringBuilder src = new StringBuilder();
        src.append("name: \"main\"\n");
        src.append("instances {\n");
        src.append("  id: \"test\"\n");
        src.append("  prototype: \"/test.go\"\n");
        src.append("  component_properties {\n");
        src.append("    id: \"test\"\n");
        src.append("    properties { id: \"number\" value: \"a\" type: PROPERTY_TYPE_NUMBER }\n");
        src.append("  }\n");
        src.append("}\n");
        @SuppressWarnings("unused")
        CollectionDesc collection = (CollectionDesc)build("/test.collection", src.toString());
    }

    private void addInstance(StringBuilder src, String id, String prototype, Point3d p, Quat4d r, double s, String ... childIds) {
        src.append("instances {\n");
        src.append("  id: \"").append(id).append("\"\n");
        src.append("  prototype: \"").append(prototype).append("\"\n");
        src.append("  position: { x: ").append(p.getX()).append(" y: ").append(p.getY()).append(" z: ").append(p.getZ()).append(" }\n");
        src.append("  rotation: { x: ").append(r.getX()).append(" y: ").append(r.getY()).append(" z: ").append(r.getZ()).append(" w: ").append(r.getW()).append(" }\n");
        src.append("  scale: ").append(s).append("\n");
        for (String childId : childIds) {
            src.append("  children: \"").append(childId).append("\"");
        }
        src.append("}\n");
    }

    private void addCollectionInstance(StringBuilder src, String id, String collection, Point3d p, Quat4d r, double s) {
        src.append("collection_instances {\n");
        src.append("  id: \"").append(id).append("\"\n");
        src.append("  collection: \"").append(collection).append("\"\n");
        src.append("  position: { x: ").append(p.getX()).append(" y: ").append(p.getY()).append(" z: ").append(p.getZ()).append(" }\n");
        src.append("  rotation: { x: ").append(r.getX()).append(" y: ").append(r.getY()).append(" z: ").append(r.getZ()).append(" w: ").append(r.getW()).append(" }\n");
        src.append("  scale: ").append(s).append("\n");
        src.append("}\n");
    }

    private static void assertEquals(Point3d v0, Point3 v1, double delta) {
        Assert.assertEquals(v0.getX(), v1.getX(), delta);
        Assert.assertEquals(v0.getY(), v1.getY(), delta);
        Assert.assertEquals(v0.getZ(), v1.getZ(), delta);
    }

    private static void assertEquals(Quat4d v0, Quat v1, double delta) {
        Assert.assertEquals(v0.getX(), v1.getX(), delta);
        Assert.assertEquals(v0.getY(), v1.getY(), delta);
        Assert.assertEquals(v0.getZ(), v1.getZ(), delta);
        Assert.assertEquals(v0.getW(), v1.getW(), delta);
    }

    /**
     * Test that a collection is flattened properly.
     * Structure:
     * - sub [collection]
     *   - sub_sub [collection]
     *     - test [instance]
     *   - test [instance]
     * - test [instance]
     * All instances have the local transform:
     * p = 1, 0, 0
     * r = 90 deg around Y
     * s = 0.5
     *
     * @throws Exception
     */
    @Test
    public void testCollectionFlattening() throws Exception {
        addFile("/test.go", "");

        Point3d p = new Point3d(1.0, 0.0, 0.0);
        Quat4d r = new Quat4d();
        r.set(new AxisAngle4d(new Vector3d(0, 1, 0), Math.PI * 0.5));
        double s = 0.5;

        StringBuilder subSubSrc = new StringBuilder();
        subSubSrc.append("name: \"sub_sub\"\n");
        addInstance(subSubSrc, "test", "/test.go", p, r, s);
        addFile("/sub_sub.collection", subSubSrc.toString());

        StringBuilder subSrc = new StringBuilder();
        subSrc.append("name: \"sub\"\n");
        addCollectionInstance(subSrc, "sub_sub", "/sub_sub.collection", p, r, s);
        addInstance(subSrc, "test", "/test.go", p, r, s);
        addFile("/sub.collection", subSrc.toString());

        StringBuilder src = new StringBuilder();
        src.append("name: \"main\"\n");
        addCollectionInstance(src, "sub", "/sub.collection", p, r, s);
        addInstance(src, "test", "/test.go", p, r, s);
        CollectionDesc collection = (CollectionDesc)build("/test.collection", src.toString());

        Assert.assertEquals(3, collection.getInstancesCount());
        Assert.assertEquals(0, collection.getCollectionInstancesCount());

        Map<String, InstanceDesc> instances = new HashMap<String, InstanceDesc>();
        for (InstanceDesc inst : collection.getInstancesList()) {
            instances.put(inst.getId(), inst);
        }
        assertTrue(instances.containsKey("/test"));
        assertTrue(instances.containsKey("/sub/test"));
        assertTrue(instances.containsKey("/sub/sub_sub/test"));

        InstanceDesc inst = instances.get("/test");
        assertEquals(new Point3d(1, 0, 0), inst.getPosition(), epsilon);
        double sq2 = Math.sqrt(2.0) * 0.5;
        assertEquals(new Quat4d(0, sq2, 0, sq2), inst.getRotation(), epsilon);
        Assert.assertEquals(0.5, inst.getScale(), epsilon);

        inst = instances.get("/sub/test");
        assertEquals(new Point3d(1, 0, -0.5), inst.getPosition(), epsilon);
        assertEquals(new Quat4d(0, 1, 0, 0), inst.getRotation(), epsilon);
        Assert.assertEquals(0.25, inst.getScale(), epsilon);

        inst = instances.get("/sub/sub_sub/test");
        assertEquals(new Point3d(0.5, 0, -0.5), inst.getPosition(), epsilon);
        assertEquals(new Quat4d(0, sq2, 0, -sq2), inst.getRotation(), epsilon);
        Assert.assertEquals(0.125, inst.getScale(), epsilon);
    }

    /**
     * Test that a collection is flattened properly w.r.t. children.
     * Structure:
     * - sub [collection]
     *   - child [instance]
     *   - parent [instance]
     * All instances have the local transform:
     * p = 1, 0, 0
     * r = 90 deg around Y
     * s = 0.5
     * Instances with no parent should have a concatenated transform, instances with a parent should retain their local transform.
     * @throws Exception
     */
    @Test
    public void testCollectionFlatteningChildren() throws Exception {
        addFile("/test.go", "");

        Point3d p = new Point3d(1.0, 0.0, 0.0);
        Quat4d r = new Quat4d();
        r.set(new AxisAngle4d(new Vector3d(0, 1, 0), Math.PI * 0.5));
        double s = 0.5;

        StringBuilder subSrc = new StringBuilder();
        subSrc.append("name: \"sub\"\n");
        addInstance(subSrc, "child", "/test.go", p, r, s);
        addInstance(subSrc, "parent", "/test.go", p, r, s, "child");
        addFile("/sub.collection", subSrc.toString());

        StringBuilder src = new StringBuilder();
        src.append("name: \"main\"\n");
        addCollectionInstance(src, "sub", "/sub.collection", p, r, s);
        CollectionDesc collection = (CollectionDesc)build("/test.collection", src.toString());

        Assert.assertEquals(2, collection.getInstancesCount());
        Assert.assertEquals(0, collection.getCollectionInstancesCount());

        Map<String, InstanceDesc> instances = new HashMap<String, InstanceDesc>();
        for (InstanceDesc inst : collection.getInstancesList()) {
            instances.put(inst.getId(), inst);
        }
        assertTrue(instances.containsKey("/sub/parent"));
        assertTrue(instances.containsKey("/sub/child"));

        InstanceDesc inst = instances.get("/sub/parent");
        assertEquals(new Point3d(1, 0, -0.5), inst.getPosition(), epsilon);
        assertEquals(new Quat4d(0, 1, 0, 0), inst.getRotation(), epsilon);
        Assert.assertEquals(0.25, inst.getScale(), epsilon);
        Assert.assertEquals("/sub/child", inst.getChildren(0));

        inst = instances.get("/sub/child");
        assertEquals(p, inst.getPosition(), epsilon);
        assertEquals(r, inst.getRotation(), epsilon);
        Assert.assertEquals(s, inst.getScale(), epsilon);
    }
}
