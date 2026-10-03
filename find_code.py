with open("/tmp/bundle.js") as f:
    s = f.read()

idx = s.find("spec-mode-higher")
# Let's search for the component definition and specMode state
state_idx = s.rfind("specMode", 0, idx)
print(f"state_idx: {state_idx}")
if state_idx != -1:
    print(s[state_idx - 100 : state_idx + 1000])
else:
    # search for 'higher' or 'lower'
    hl_idx = s.rfind('"higher"', 0, idx)
    print(f"hl_idx: {hl_idx}")
    if hl_idx != -1:
        print(s[hl_idx - 200 : hl_idx + 800])
