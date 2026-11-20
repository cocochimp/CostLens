import json, sys, subprocess, itertools

def load(path):
    out = subprocess.run(
        ["node", "/Users/lovin/.agents/skills/archify/bin/archify.mjs",
         "validate", "architecture", path, "--layout-json"],
        capture_output=True, text=True, cwd="/Users/lovin/Desktop/AgentTest/xxx01")
    return json.loads(out.stdout)

def segs(pts):
    return [(tuple(pts[i]), tuple(pts[i+1])) for i in range(len(pts)-1)]

def cross(a, b, c, d):
    def orient(p, q, r):
        return (q[0]-p[0])*(r[1]-p[1]) - (q[1]-p[1])*(r[0]-p[0])
    o1, o2 = orient(a, b, c), orient(a, b, d)
    o3, o4 = orient(c, d, a), orient(c, d, b)
    eps = 1e-9
    if abs(o1) < eps or abs(o2) < eps or abs(o3) < eps or abs(o4) < eps:
        return False
    return (o1 > 0) != (o2 > 0) and (o3 > 0) != (o4 > 0)

def main(path):
    d = load(path)
    conns = d["connections"]
    total = 0
    pairs = []
    for x, y in itertools.combinations(conns, 2):
        if x["from"] == y["from"] and x["to"] == y["to"]:
            continue
        shared = {x["from"], x["to"]} & {y["from"], y["to"]}
        hits = 0
        for s1 in segs(x["points"]):
            for s2 in segs(y["points"]):
                if cross(s1[0], s1[1], s2[0], s2[1]):
                    hits += 1
        if hits:
            total += hits
            pairs.append((x.get("id") or f'{x["from"]}->{x["to"]}', y.get("id") or f'{y["from"]}->{y["to"]}', hits, sorted(shared)))
    print("viewBox", d["viewBox"], "crossings", total)
    for p in pairs:
        print("  ", p)

main(sys.argv[1])