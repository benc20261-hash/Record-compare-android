with open("/tmp/bundle.js") as f:
    s = f.read()

idx = s.find("spec-mode-higher")
# Let's see the component header
comp_idx = s.rfind("function ", 0, idx - 1000)
print("Slice before spec-mode-higher:")
print(s[idx - 3500 : idx + 1000])
