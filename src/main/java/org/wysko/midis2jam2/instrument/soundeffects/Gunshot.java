package org.wysko.midis2jam2.instrument.soundeffects;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import org.jetbrains.annotations.NotNull;
import org.wysko.midis2jam2.Midis2jam2;
import org.wysko.midis2jam2.instrument.DecayedInstrument;
import org.wysko.midis2jam2.instrument.TwelveDrumOctave;
import org.wysko.midis2jam2.midi.MidiChannelSpecificEvent;
import org.wysko.midis2jam2.midi.MidiNoteOnEvent;

import java.util.ArrayList;
import java.util.List;

import static org.wysko.midis2jam2.Midis2jam2.rad;

public class Gunshot extends DecayedInstrument {
	
	Pistol[] pistols = new Pistol[12];
	
	Node[] gunNodes = new Node[12];
	
	/**
	 * @param context   the context to the main class
	 * @param eventList the event list
	 */
	public Gunshot(@NotNull Midis2jam2 context,
	               @NotNull List<MidiChannelSpecificEvent> eventList) {
		super(context, eventList);
		
		for (int i = 0; i < 12; i++) {
			gunNodes[i] = new Node();
			pistols[i] = new Pistol();
			Node highestLevel = pistols[i].highestLevel;
			gunNodes[i].attachChild(highestLevel);
			instrumentNode.attachChild(gunNodes[i]);
			highestLevel.setLocalTranslation(0, 45, -140);
			gunNodes[i].setLocalRotation(new Quaternion().fromAngles(0, rad(i * 1.5), 0));
		}
	}
	
	@Override
	public void tick(double time, float delta) {
		super.tick(time, delta);
		List<MidiNoteOnEvent> ons = new ArrayList<>();
		while (!hits.isEmpty() && context.file.eventInSeconds(hits.get(0)) <= time) {
			ons.add(hits.remove(0));
		}
		for (MidiNoteOnEvent on : ons) {
			int i = (on.note + 3) % 12;
			pistols[i].fire();
		}
		for (Pistol pistol : pistols) {
			pistol.tick(time, delta);
		}
	}
	
	@Override
	protected void moveForMultiChannel() {
		offsetNode.setLocalRotation(new Quaternion().fromAngles(0, rad(-70 + 20 * indexForMoving()), 0));
	}
	
	public class Pistol extends TwelveDrumOctave.TwelfthOfOctaveDecayed {
		
		private final Spatial blast;
		
		private boolean firedOnce = false;
		
		private float scale = 0;
		
		public Pistol() {
			Spatial pistol = context.loadModel("Pistol.obj", "PistolSkin.png");
			blast = context.shadow("Assets/PistolBlast.fbx", "Assets/Explosion.png");
			blast.setLocalScale(0);
			blast.setLocalTranslation(0, 3, 5.5f);
			Node modelNode = new Node();
			modelNode.attachChild(pistol);
			highestLevel.attachChild(blast);
			animNode.attachChild(modelNode);
		}
		
		@Override
		public void tick(double time, float delta) {
			Vector3f localTranslation = animNode.getLocalTranslation();
			Quaternion localRotation = animNode.getLocalRotation();
			blast.setLocalScale(Math.max(0, blast.getLocalScale().x - delta * 10));
			
			if (firedOnce) {
				blast.setLocalScale(scale);
				scale += delta * 30;
				((Geometry) ((Node) ((Node) blast).getChild(0)).getChild(0)).getMaterial().setFloat(
						"AlphaDiscardThreshold", scale / 7f);
			}
			
			if (localTranslation.z < 0.001) {
				animNode.move(0, 0, delta * 30);
				Vector3f newTranslation = animNode.getLocalTranslation();
				animNode.setLocalTranslation(newTranslation.setZ(Math.min(newTranslation.getZ(), 0)));
			} else {
				animNode.setLocalTranslation(0, 0, 0);
			}
			if (Math.toDegrees(localRotation.toAngles(null)[0]) < 0.001) {
				animNode.rotate(rad(3), 0, 0);
				float v = animNode.getLocalRotation().toAngles(null)[0];
				animNode.setLocalRotation(new Quaternion().fromAngles(Math.min(v, 0), 0, 0));
			}
		}
		
		public void fire() {
			animNode.setLocalTranslation(0, 0, -5);
			animNode.setLocalRotation(new Quaternion().fromAngles(rad(-30), 0, 0));
			firedOnce = true;
			scale = 0;
		}
	}
}