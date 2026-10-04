"""Eigenstaendige Vorder- und Rueckansichten fuer die sechs Spielwesen (64x64).

Die bestehenden Seitenposen bleiben unangetastet. Zwei kurze Schrittphasen teilen denselben
Bodenanker, damit der Wechsel beim Hoch-/Runtergehen weder springt noch das Gesicht spiegelt.
"""
from sprite import Sprite, Pose
import characters as C


def draw(name, back=False, step=-1):
    s = Sprite(pose=Pose(step=step))
    foot = lambda i: s.pose.foot_lift(i)
    if name == 'fennec':
        fur, dark, cloth = C.FUR_ORANGE, C.DARK_PAW, C.MANTLE
        s.part(s.ellipse(32, 48, 14, 12), fur, fur=.35)
        for i, x in enumerate((25, 39)):
            s.part(s.ellipse(x, 59-foot(i), 5, 2.5), dark)
        s.part(s.poly([(18, 21), (10, 2), (27, 15)]), fur)
        s.part(s.poly([(46, 21), (54, 2), (37, 15)]), fur)
        if not back:
            s.part(s.ellipse(20, 18, 3.5, 7), C.PINK_EAR, line=False)
            s.part(s.ellipse(44, 18, 3.5, 7), C.PINK_EAR, line=False)
        s.part(s.ellipse(32, 32, 14, 12), fur, fur=.25)
        if back:
            s.part(s.poly([(20, 38), (44, 38), (48, 57), (16, 57)]), cloth)
            s.paint(s.pen().line([(32, 40), (32, 55)]).m, '#a76b4b')
            s.part(s.ellipse(32, 55, 7, 4), C.CREAM, line=False)
        else:
            s.part(s.ellipse(32, 37, 9, 6), C.CREAM, line=False)
            for x in (25, 39): s.eye(x-2, 30, 5, 5, iris='#35251e')
            s.px(32, 37, '#35251e')
            s.part(s.poly([(18, 42), (46, 42), (43, 56), (21, 56)]), cloth)
            s.part(s.ellipse(32, 43, 2, 2), ['#3b615d', '#66a6a0'], line=False)
    elif name == 'gloop':
        s.part(s.ellipse(32, 43, 22, 17) | s.ellipse(32, 35, 17, 17), C.MOSS, fur=.12)
        s.part(s.ellipse(32, 56, 18, 4), C.MOSS_DARK, line=False)
        if back:
            s.part(s.poly([(15, 40), (47, 42), (45, 52), (22, 50)]), C.MOSS_DARK, line=False)
            s.part(s.ellipse(43, 49, 5, 6), ['#6e573e', '#9a7957'], line=False)
        else:
            s.part(s.ellipse(32, 45, 13, 10), C.MOSS[2:], line=False)
            for x in (23, 41): s.eye(x-2, 38, 5, 6, iris='#243225')
            s.px(32, 48, '#35493b')
        s.part(s.ellipse(32, 19, 2, 7), C.SPROUT)
        s.part(s.ellipse(26, 16, 7, 3, rot=-.5) | s.ellipse(38, 14, 7, 3, rot=.5), C.SPROUT)
    elif name == 'starlet':
        import math
        pts=[]
        for i in range(10):
            a=-math.pi/2+i*math.pi/5
            r=27 if i%2==0 else 13
            pts.append((32+math.cos(a)*r, 34+math.sin(a)*r))
        s.part(s.poly(pts), C.GOLD, bulge=1.4)
        s.part(s.poly([(20, 47), (44, 47), (41, 55), (23, 55)]), ['#3e4c6a','#536783','#8a9cb5'])
        if back:
            s.paint(s.pen().line([(26, 49),(37, 52)]).m, '#d2b878')
        else:
            for x in (24, 40): s.eye(x-2, 34, 5, 6, iris='#3a2214')
            s.px(32, 44, '#8a4a20')
    elif name == 'puffling':
        cloud = s.ellipse(32, 43, 19, 16)
        for x,y,r in ((16,36,9),(48,36,9),(24,26,10),(40,26,10),(32,52,12)):
            cloud |= s.ellipse(x,y,r,r)
        s.part(cloud, C.CLOUD, fur=.5)
        for i,x in enumerate((25,39)): s.part(s.ellipse(x,59-foot(i),4,2.5),C.CLOUD_BLUE)
        if back:
            s.part(s.ellipse(32,35,13,12),C.HOOD)
            s.part(s.ellipse(38,48,5,6),C.SATCHEL)
        else:
            for x in (24,40): s.eye(x-2,34,5,6,iris='#1e2a48')
            s.px(32,43,'#8a6a5a')
            s.part(s.ellipse(32,49,6,4),C.HOOD,line=False)
    elif name == 'wyrmling':
        s.part(s.poly([(24,39),(10,18),(4,27),(15,43)]),C.WING)
        s.part(s.poly([(40,39),(54,18),(60,27),(49,43)]),C.WING)
        s.part(s.ellipse(32,46,12,14),C.JADE)
        for i,x in enumerate((24,40)): s.part(s.ellipse(x,59-foot(i),5,3),C.JADE[:3])
        s.part(s.poly([(23,20),(19,6),(25,9),(29,20)]),C.HORN)
        s.part(s.poly([(41,20),(45,6),(39,9),(35,20)]),C.HORN)
        s.part(s.ellipse(32,29,13,12),C.JADE)
        if back:
            s.part(s.poly([(24,38),(40,38),(45,53),(19,53)]),C.JADE[:4],line=False)
            s.paint(s.pen().line([(32,39),(32,51)]).m,'#a8724a')
            s.part(s.ellipse(32,57,5,3),C.JADE)
        else:
            s.part(s.ellipse(32,48,7,9),C.CREAM_BELLY,line=False)
            for x in (24,40): s.eye(x-2,28,5,6,iris='#3a2008')
            s.px(32,38,'#2c4038')
            s.part(s.ellipse(32,41,8,2),C.WRAP,line=False)
    elif name == 'hootlet':
        s.part(s.ellipse(32,43,17,17),C.VIOLET,fur=.4)
        s.part(s.poly([(18,29),(13,13),(24,24)]),C.VIOLET)
        s.part(s.poly([(46,29),(51,13),(40,24)]),C.VIOLET)
        for i,x in enumerate((25,39)): s.part(s.ellipse(x,59-foot(i),4,2.5),C.BEAK)
        if back:
            s.part(s.poly([(22,35),(42,35),(49,55),(15,55)]),C.CLOAK)
            s.paint(s.pen().line([(32,36),(32,54)]).m,'#b69761')
        else:
            s.part(s.ellipse(25,32,9,9)|s.ellipse(39,32,9,9),C.FACE,line=False)
            for x in (24,40): s.eye(x-2,30,5,6,iris='#2a1e14')
            s.part(s.poly([(29,37),(35,37),(32,43)]),C.BEAK,line=False)
            s.part(s.ellipse(32,47,8,6),C.FACE,line=False)
    else:
        raise ValueError(name)
    s.outline()
    return s.image()
