# Character and vehicles

The fan-made procedural hedgehog has swept blue quills, green eyes, tan muzzle, white gloves and red shoes. Geometry is generated in Java; no official or extracted Sega assets are bundled.

Head, shoulders, elbows, hips and knees form a rigid-part hierarchy. Running alternates limbs and leans forward; jumping curls into a spin. Every new run begins on foot. Collecting a bike, car or surfboard selects its model and riding pose. Q restores running.

The bike has two animated wheels, forks and handlebars. The convertible has four animated wheels, a body, lights and windshield. The surfboard has a tapered deck, stripe and fin, with a balancing rider pose. Pickup models are rendered on the route before collection.

HedgehogRig builds meshes and poses. Renderer3D.follow maps them onto the banked, elevated track. These are procedural arcade models, without weighted skin deformation, realistic suspension or water simulation.

## Native kart roster

Turbo Tails reuses this same rig and car geometry. The default Sonic runner constructor is unchanged. Kart variants add Tails' twin tails, tall ears and white cheek tufts; Knuckles' red locks, chest marking and glove spikes; and Amy's pink quills, headband and eyelashes. Each has its own kart livery. Drivers use a seated steering pose, animated wheels and corner lean. Full-detail static portraits are generated from these actual meshes; racing uses reduced mesh detail with distance.
