package com.notime.glyphsim.matrix

import com.notime.glyphsim.matrix.PlayScene.Place
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class GamePhysicsTest {
    @Test fun `Frueheres Schwimmgebiet hat nur trockenen Laufboden und feste Vordergrenze`() {
        for(place in listOf(Place.POND,Place.BEACH,Place.SWAMP,Place.JUNGLE)) {
            val scene=GameWorld.scene(place)!!
            for(i in 0..20) {
                val x=i/20f
                val safe=GameTerrain.clamp(scene,PlayControl.Pos(x,1f))
                assertTrue(GameTerrain.valid(scene,safe))
                assertTrue(GameScenes.feet(scene,safe).second<=562.001f)
                assertFalse(GameTerrain.valid(scene,GameScenes.posAt(scene,x*480f,600f)))
                val feet=GameScenes.feet(scene,safe)
                assertNotEquals(GameEnvironment.Material.WATER,GameWorld.material(scene,feet.first,feet.second))
            }
        }
    }
    @Test fun `Alte Schwimmposition bleibt lesbar und wird beim Einstieg auf Land begrenzt`() {
        val original=GameAdventure.State(place=Place.BEACH,pos=PlayControl.Pos(.5f,.95f))
        val decoded=GameAdventure.decode(GameAdventure.encode(original))
        assertEquals(original,decoded)
        val safe=GameAdventure.safePosition(decoded.place,decoded.pos)
        assertTrue(GameTerrain.valid(GameWorld.scene(decoded.place)!!,safe))
        assertTrue(safe.depth<decoded.pos.depth)
        assertEquals(original.backpack,decoded.backpack)
    }
    @Test fun `Trockener Kuestenweg ist in beiden Richtungen durchgehend begehbar`() {
        for(place in listOf(Place.POND,Place.BEACH,Place.SWAMP,Place.JUNGLE)) {
            val scene=GameWorld.scene(place)!!
            for(forward in listOf(true,false)) {
                val y=when(place) { Place.POND -> 550f; Place.JUNGLE -> 500f; else -> 530f }
                var pos=GameTerrain.clamp(scene,PlayControl.Pos(if(forward) .02f else .98f,(y-scene.farY)/(scene.nearY-scene.farY)))
                val target=GameTerrain.clamp(scene,pos.copy(x=if(forward) .98f else .02f))
                repeat(600) {
                    val next=GameSurfaces.approach(scene,pos,target,50L)
                    assertTrue(GameTerrain.valid(scene,next));pos=next
                }
                assertEquals("$place/$forward",target,pos)
            }
        }
    }
    @Test fun `Sperrinsel wird auch zwischen zwei legalen Endpunkten erkannt`() {
        val scene=GameWorld.scene(Place.PARK)!!
        val from=GameScenes.posAt(scene,230f,550f)
        val to=GameScenes.posAt(scene,330f,550f)
        assertTrue(GameTerrain.valid(scene,from));assertTrue(GameTerrain.valid(scene,to))
        assertFalse(GameWalkingMap.clear(scene,from,to))
        val stopped=GameTerrain.resolve(scene,from,to)
        assertTrue(GameTerrain.valid(scene,stopped))
        assertTrue(GameWalkingMap.clear(scene,from,stopped))
        assertNotEquals(to,stopped)
    }
    @Test fun `Anlauf umgeht die Felsinsel ohne durch sie hindurchzugehen`() {
        val scene=GameWorld.scene(Place.PARK)!!
        var pos=GameScenes.posAt(scene,230f,550f)
        val target=GameScenes.posAt(scene,390f,550f)
        repeat(300) {
            val next=GameSurfaces.approach(scene,pos,target,50)
            assertTrue(GameWalkingMap.clear(scene,pos,next));pos=next
        }
        assertEquals(target,pos)
    }
    @Test fun `Rennen kann kein schmales Moebel durchqueren`() {
        val from=PlayControl.Pos(.4f,.5f)
        val thin=GameMovement.Surface("thin-bench",.4003f,.4006f,.4f,.6f,30f)
        val next=GameMovement.tick(GameMovement.State(pos=from),PlayControl.Stick(1f,0f),50,listOf(thin)).state
        assertTrue(next.pos.x<thin.x0);assertNull(next.support)
    }

    @Test fun `Nur vermessenes Gras reagiert und federt nach dem Schritt zurueck`() {
        val scene=GameWorld.scene(Place.PARK)!!
        val x=100f;val y=515f
        assertEquals(GameEnvironment.Material.GRASS,GameWorld.material(scene,x,y))
        assertNotEquals(GameEnvironment.Material.GRASS,GameWorld.material(scene,250f,y))
        val patch=GameAtmosphere.vegetationPatches(scene).first()
        val contact=GameEnvironment.Contact(scene.place,x,y,GameEnvironment.Material.GRASS,0,1200,0,1f,0f)
        val fresh=GameEnvironment.State(clock=0,contacts=listOf(contact))
        val bent=GameAtmosphere.vegetationOffset(scene,patch,x,y-4f,fresh)
        assertTrue(bent>0f)
        assertTrue(GameAtmosphere.vegetationOffset(scene,patch,x,y-4f,fresh.copy(clock=600))<bent)
        assertEquals(0f,GameAtmosphere.vegetationOffset(scene,patch,x,y-4f,fresh.copy(clock=1200)),0f)
        assertEquals(0f,GameAtmosphere.vegetationOffset(scene,patch,patch.x-4f,y,fresh),0f)
        assertEquals(0f,GameAtmosphere.vegetationOffset(scene,patch,x,y-4f,
            fresh.copy(contacts=listOf(contact.copy(material=GameEnvironment.Material.STONE)))),0f)
    }
    @Test fun `Kompakter Innenraumblick schneidet leeren Vorderboden ab`() {
        val scene=GameWorld.scene(Place.LIVING)!!;val pos=PlayControl.Pos(.5f,.6f)
        val camera=GameCamera.tick(GameCamera.State(),scene,pos,PlayControl.Stick(),640f,360f,0)
        val fit=GameCamera.fit(camera,scene,640f,360f)
        assertTrue(fit.toScreen(240f,260f).second>360f)
        assertTrue(fit.toScreen(240f,180f).second in 0f..360f)
        assertTrue(GameTerrain.valid(scene,GameScenes.posAt(scene,240f,220f)))
        assertFalse(GameTerrain.valid(scene,PlayControl.Pos(.5f,1.05f)))
        assertFalse(GameTerrain.valid(scene,GameScenes.posAt(scene,240f,130f)))
    }


    @Test fun `Ein UI Sprung wird nach dem Positionsabgleich im Motorbild ausgefuehrt`() {
        val scene=GameWorld.scene(PlayScene.Place.PARK)!!
        val surfaces=GameSurfaces.painted(scene)
        val pos=PlayControl.Pos(.5f,.6f)
        var state=GameTerrain.tick(scene,AvatarSpecies.FENNEC,GameMovement.State(pos=pos),PlayControl.Stick(),16L,
            surfaces,GameMovement.Command.JUMP to PlayControl.Stick()).state
        assertEquals(GameMovement.Action.JUMP,state.action)
        repeat(20) { state=GameTerrain.tick(scene,AvatarSpecies.FENNEC,state,PlayControl.Stick(),16L,surfaces).state }
        assertTrue(state.height>20f)
        repeat(30) { state=GameTerrain.tick(scene,AvatarSpecies.FENNEC,state,PlayControl.Stick(),16L,surfaces).state }
        assertNull(state.action)
    }
    @Test fun `Alle Tuerblaetter lesen das sichtbare Anschlussbild und einen gueltigen Ausschnitt`() {
        for(passage in Place.entries.flatMap { GameWorld.passages(it) }.filter { it.door }) {
            val sample=GameDoors.sample(passage)!!
            val box=GameDoors.aperture(passage.from,passage.to)
            assertTrue(box.x0+sample.offset>=0f)
            assertTrue(box.x1+sample.offset<=sample.width)
            assertTrue(box.y0>=0f && box.y1<=sample.height)
            if(GameWorld.contains(passage.from)) {
                val seam=GameWorld.seams.first { it.asset==sample.asset }
                assertEquals(GameWorld.origin(passage.from)+box.x0-(seam.x-GameWorld.SEAM_HALF),
                    box.x0+sample.offset,.001f)
                assertNotEquals(GameWorld.scene(passage.from)!!.asset,sample.asset)
            } else assertEquals(GameWorld.scene(passage.from)!!.asset,sample.asset)
        }
    }
    @Test fun `Automatischer Anlauf folgt dem Brueckenboden statt durch den Fluss zu schneiden`() {
        val scene=GameWorld.scene(Place.COAST_PATH)!!
        var pos=GameTerrain.clamp(scene,PlayControl.Pos(.04f,.85f))
        val target=GameTerrain.clamp(scene,PlayControl.Pos(.92f,.85f))
        repeat(1000) {
            pos=GameSurfaces.approach(scene,pos,target,50)
            assertTrue("Brueckenanlauf $pos",GameTerrain.valid(scene,pos))
        }
        assertEquals(target,pos)
    }
    @Test fun `Laufboden endet vor Felskanten und niemals im Himmel`() {
        for(place in GameWorld.places) {
            val scene=GameWorld.scene(place)!!
            for(ix in 0..20) for(depth in listOf(0f,.5f,1f)) {
                val p=GameTerrain.clamp(scene,PlayControl.Pos(ix/20f,depth))
                val feet=GameScenes.feet(scene,p)
                val band=GameTerrain.band(scene,p.x)
                assertTrue("$place $p",feet.second>=band.first-.001f && feet.second<=band.second+.001f)
                assertTrue(feet.second >= if (place == Place.COAST_PATH) 370f else 400f)
                assertEquals(p,GameTerrain.clamp(scene,p))
            }
        }
    }
    @Test fun `Laufkanten bleiben an allen Landschaftsnaehten stetig`() {
        for((a,b) in GameWorld.places.zipWithNext()) for(d in listOf(0f,.5f,1f)) {
            val sa=GameWorld.scene(a)!!; val sb=GameWorld.scene(b)!!
            val pa=GameTerrain.clamp(sa,PlayControl.Pos(1f,d));val pb=GameTerrain.clamp(sb,PlayControl.Pos(0f,d))
            assertEquals("$a $b",GameWorld.feet(a,pa).second,GameWorld.feet(b,pb).second,.002f)
            assertEquals(GameScenes.avatarHeight(sa,pa),GameScenes.avatarHeight(sb,pb),.002f)
        }
    }
    @Test fun `Alte Position bleibt im Codec erhalten und wird erst beim Betreten auf Boden gesetzt`() {
        val pond=GameAdventure.State(place=Place.POND,pos=PlayControl.Pos(.5f,.25f))
        assertEquals(pond,GameAdventure.decode(GameAdventure.encode(pond)))
        val original=GameAdventure.State(place=Place.PARK,pos=PlayControl.Pos(.5f,.9f))
        val decoded=GameAdventure.decode(GameAdventure.encode(original))
        assertEquals(original,decoded)
        val safe=GameAdventure.safePosition(decoded.place,decoded.pos)
        assertTrue(GameTerrain.valid(GameWorld.scene(decoded.place)!!,safe))
        assertTrue(safe.depth<decoded.pos.depth)
        assertEquals(original.backpack,decoded.backpack)
    }



    @Test fun `Kuestenweg hat auf beiden Tiefen dasselbe Tempo`() {
        val scene=GameWorld.scene(Place.BEACH)!!;val species=AvatarSpecies.FENNEC
        fun travel(depth: Float): Float {
            var state=GameMovement.State(pos=GameTerrain.clamp(scene,PlayControl.Pos(.35f,depth)))
            val start=GameScenes.feet(scene,state.pos).first
            repeat(10) { state=GameTerrain.tick(scene,species,state,PlayControl.Stick(.4f,0f),50,emptyList()).state }
            return GameScenes.feet(scene,state.pos).first-start
        }
        assertEquals(travel(.25f),travel(.9f),.001f)
    }
    @Test fun `Sprung durch hohen Gegenstand stoppt und faellt auf Boden`() {
        val obstacle=GameMovement.Surface("wall",.48f,.51f,0f,1f,160f)
        var s=GameMovement.State(pos=PlayControl.Pos(.4f,.5f),action=GameMovement.Action.JUMP,
            arc=GameMovement.Arc(PlayControl.Pos(.4f,.5f),PlayControl.Pos(.6f,.5f),0f,0f,null))
        repeat(25) { s=GameMovement.tick(s,PlayControl.Stick(),50,listOf(obstacle),sweptJumpCollision=true).state }
        assertTrue(s.pos.x<.48f);assertEquals(0f,s.height,.001f);assertNull(s.action)
    }
    @Test fun `Sprung hebt Ohren nicht durch die Zimmerdecke`() {
        val scene=GameWorld.scene(Place.LIVING)!!
        for(species in AvatarSpecies.entries) {
            var state=GameMovement.command(GameMovement.State(pos=PlayControl.Pos(.5f,.05f)),
                GameMovement.Command.JUMP,PlayControl.Stick(),emptyList())
            repeat(15) {
                state=GameTerrain.tick(scene,species,state,PlayControl.Stick(),50,emptyList()).state
                val head=GameScenes.feet(scene,state.pos).second-GameCharacterScale.visibleHeight(scene,state.pos,species)-state.height
                assertTrue("$species $head",head>=19.999f)
            }
        }
    }
    @Test fun `Alle Tueren haben gemalte Trefferflaeche freien Zugang und Rueckweg`() {
        for(place in Place.entries) for(door in GameWorld.passages(place).filter { it.door }) {
            val scene=GameWorld.scene(place)!!;val box=GameDoors.aperture(place,door.to)
            assertEquals(box,door.hit(scene));assertTrue(box.x1>box.x0 && box.y1>box.y0)
            val target=GameAdventure.safePosition(place,door.pos)
            assertTrue(GameTerrain.valid(scene,target))
            var p=GameAdventure.safePosition(place,PlayControl.Pos(.5f,.6f))
            repeat(1800) { p=GameSurfaces.approach(scene,p,target,16) }
            assertEquals("$place -> ${door.to}",target.x,p.x,.001f)
            assertEquals(target.depth,p.depth,.001f)
            assertTrue(GameWorld.passages(door.to).any { it.to==place && it.door })
        }
    }
    @Test fun `Tuer bewegt sich nur in aktiver Phase und laesst sich vollstaendig schliessen`() {
        val door=GameWorld.passages(Place.LIVING).first { it.door }
        val opening=GameDoors.State(door,100)
        assertEquals(0f,GameDoors.opening(opening,door,100),0f)
        assertEquals(.5f,GameDoors.opening(opening,door,310),.001f)
        assertEquals(1f,GameDoors.opening(opening,door,520),0f)
        val reverse=GameWorld.passages(door.to).first { it.to==door.from }
        assertEquals(0f,GameDoors.opening(opening,reverse,310),0f)
        val closing=GameDoors.State(reverse,520,true)
        assertEquals(1f,GameDoors.opening(closing,reverse,520),0f)
        assertEquals(0f,GameDoors.opening(closing,reverse,840),0f)
        assertEquals(0f,GameDoors.opening(null,door,1000),0f)
    }

    @Test fun `Randdruck hat weder Laufzyklus noch neue Schrittspuren`() {
        val scene=GameWorld.scene(Place.PARK)!!
        val p=GameTerrain.clamp(scene,PlayControl.Pos(.5f,1f))
        var state=GameMovement.State(pos=p,velocity=PlayControl.Stick(0f,1f),gaitMs=10.0,running=true)
        var env=GameEnvironment.State(place=scene.place)
        repeat(20) {
            val next=GameTerrain.tick(scene,AvatarSpecies.FENNEC,state,PlayControl.Stick(0f,1f),16,emptyList()).state
            env=GameEnvironment.tick(env,scene,state,next,16,null)
            state=next
        }
        assertFalse(state.moving);assertFalse(state.running);assertEquals(10.0,state.gaitMs,0.0)
        assertEquals(p,state.pos);assertTrue(env.contacts.isEmpty())
    }
    @Test fun `Luftaufprall erzeugt keinen Kontakt am nie erreichten Sprungziel`() {
        val scene=GameWorld.scene(Place.BEACH)!!
        val from=PlayControl.Pos(.4f,.8f);val target=PlayControl.Pos(.6f,.8f)
        val before=GameMovement.State(pos=PlayControl.Pos(.45f,.8f),height=20f,
            action=GameMovement.Action.JUMP,elapsed=250,arc=GameMovement.Arc(from,target,0f,0f,null))
        val stopped=before.copy(elapsed=0,arc=GameMovement.Arc(before.pos,before.pos,20f,0f,null,330,0f))
        val env=GameEnvironment.State(place=scene.place)
        assertTrue(GameEnvironment.tick(env,scene,before,stopped,16,null).contacts.isEmpty())
        val falling=stopped.copy(elapsed=320)
        val landed=falling.copy(action=null,arc=null,height=0f)
        val contact=GameEnvironment.tick(env,scene,falling,landed,16,null).contacts.single()
        assertEquals(GameScenes.feet(scene,landed.pos).first,contact.x,.001f)
        assertEquals(GameScenes.feet(scene,landed.pos).second,contact.y,.001f)
    }

    @Test fun `Kuechenflur verwendet wirkliche Tuer statt Kuehlschrankwand`() {
        val doors=GameWorld.passages(Place.KITCHEN).filter { it.door }
        assertEquals(2,doors.size)
        assertEquals(GameDoors.aperture(Place.KITCHEN,Place.LIVING),GameDoors.aperture(Place.KITCHEN,Place.CRAFT))
        assertEquals(setOf(Place.LIVING,Place.CRAFT),GameDoors.choices(doors.first()).map { it.to }.toSet())
        val scene=GameWorld.scene(Place.BEACH)!!
        val state=GameMovement.State(pos=GameTerrain.clamp(scene,PlayControl.Pos(.5f,.9f)))
        assertEquals(GameMovement.Action.ROLL,GameTerrain.command(scene,state,GameMovement.Command.ROLL,PlayControl.Stick(),emptyList()).action)
        assertEquals(GameMovement.Action.SIT,GameTerrain.command(scene,state,GameMovement.Command.REST,PlayControl.Stick(),emptyList()).action)
    }

    @Test fun `Vorladereihenfolge enthaelt alle Bilder einmal und beginnt am gespeicherten Ort`() {
        val all=(GameWorld.regions.map { it.asset }+GameWorld.seams.map { it.asset }+
            GameInteriorCatalog.scenes.values.map { it.asset }).toSet()
        for(place in Place.entries) {
            val paths=GameAssetPlan.paths(place)
            assertEquals(GameWorld.scene(place)!!.asset,paths.first())
            assertEquals(all,paths.toSet());assertEquals(paths.size,paths.distinct().size)
        }
    }
    @Test fun `Echtes Rennen fuehrt zu kurzer Pause und erholt sich ohne Speicherwert`() {
        var breath=GameBreath.State()
        repeat(245) { breath=GameBreath.tick(breath,true,true,50) }
        assertTrue(breath.pauseLeft>0L)
        assertEquals(PlayControl.Stick(),GameBreath.input(breath,PlayControl.Stick(1f,0f)))
        assertNotNull(GameBreath.cue(breath))
        repeat(80) { breath=GameBreath.tick(breath,false,false,50) }
        assertEquals(0L,breath.pauseLeft);assertFalse(breath.recovering)
        assertEquals(PlayControl.Stick(1f,0f),GameBreath.input(breath,PlayControl.Stick(1f,0f)))
        assertNull(GameBreath.cue(breath))
    }
    @Test fun `Wanddruck und langsames Gehen kosten keine Puste`() {
        var breath=GameBreath.State()
        repeat(1000) { breath=GameBreath.tick(breath,false,it%2==0,16) }
        assertEquals(GameBreath.State(),breath)
        assertEquals(breath,GameBreath.tick(breath,true,true,0))
        val limited=GameBreath.input(GameBreath.State(.2f,0,true),PlayControl.Stick(.8f,.6f))
        assertEquals(.65f,limited.strength,.0001f)
        assertEquals(4f/3f,limited.x/limited.y,.0001f)
    }

    @Test fun `Brueckenkrone liegt auf Laufdeck oberhalb des Steinbogens`() {
        val scene = GameWorld.scene(Place.COAST_PATH)!!
        for (depth in listOf(0f, .5f, 1f)) {
            val feet = GameScenes.feet(scene, GameTerrain.clamp(scene, PlayControl.Pos(.43f, depth)))
            assertTrue("Fuss auf Brueckenwand: $feet", feet.second in 370f..396f)
        }
    }
    @Test fun `Manuelles Gehen und Rennen ueberquert Bruecke in beide Richtungen`() {
        val scene = GameWorld.scene(Place.COAST_PATH)!!
        for (direction in listOf(-1f, 1f)) for (strength in listOf(.55f, 1f)) {
            var state = GameMovement.State(pos = GameTerrain.clamp(scene,
                PlayControl.Pos(if (direction > 0) .03f else .97f, .6f)))
            var crossedCrest = false
            repeat(2400) {
                state = GameTerrain.tick(scene, AvatarSpecies.FENNEC, state,
                    PlayControl.Stick(direction * strength, 0f), 16, GameSurfaces.painted(scene)).state
                assertTrue(GameTerrain.valid(scene, state.pos))
                if (state.pos.x in .42f.. .44f) {
                    crossedCrest = true
                    assertTrue(GameScenes.feet(scene, state.pos).second < 400f)
                }
            }
            assertTrue(crossedCrest)
            assertEquals(if (direction > 0) 1f else 0f, state.pos.x, .001f)
        }
    }
    @Test fun `Sichtbarer Shop und Tuerschrift reagieren auch aus Nachbarabschnitt`() {
        val current = GameWorld.scene(Place.PARK)!!
        val shop = GameWorld.passages(Place.STREET).first { it.to == Place.SHOP }
        val scene = GameWorld.scene(shop.from)!!
        val feet = GameScenes.feet(scene, shop.pos)
        assertEquals(shop, GameWorld.passageAtVisible(current, GameWorld.origin(shop.from) + feet.first, feet.second - 70f))
        assertEquals(shop, GameWorld.passageAtVisible(current, GameWorld.origin(shop.from) + feet.first + 28f, feet.second - 70f))
        assertNull(GameWorld.passageAtVisible(current, GameWorld.origin(shop.from) + feet.first, 300f))
        assertNull(GameWorld.passageAtVisible(GameWorld.scene(Place.BEDROOM)!!, feet.first, feet.second - 70f))
    }


    @Test fun `Tuer reagiert auch im Bodensitzen waehrend laufende Spruenge warten`() {
        assertTrue(GameDoors.canEnter(GameMovement.State(action = GameMovement.Action.REST)))
        assertTrue(GameDoors.canEnter(GameMovement.State(action = GameMovement.Action.SIT)))
        for (action in listOf(GameMovement.Action.JUMP, GameMovement.Action.ROLL, GameMovement.Action.RISE))
            assertFalse(GameDoors.canEnter(GameMovement.State(action = action)))
        assertFalse(GameDoors.canEnter(GameMovement.State(height = 22f, support = "table")))
    }

}
