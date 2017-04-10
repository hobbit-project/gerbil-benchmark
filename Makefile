default: build dockerize

build:
	cd gerbil-benchmark && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-data-generator && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-evaluation-module && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-task-generator && mvn clean package -U -Dmaven.test.skip=true

dockerize:
	cd gerbil-benchmark && docker build -f gerbil_controller.docker -t git.project-hobbit.eu:4567/gerbil/gerbilcontroller .
	cd gerbil-data-generator && docker build -f gerbil_data_generator.docker -t git.project-hobbit.eu:4567/gerbil/gerbildatagenerator .
	cd gerbil-task-generator && docker build -f gerbil_task_generator.docker -t git.project-hobbit.eu:4567/gerbil/gerbiltaskgenerator .
	cd gerbil-evaluation-module && docker build -f gerbil_evaluation_module.docker -t git.project-hobbit.eu:4567/gerbil/gerbil-evaluation-module .
	cd gerbil-benchmark && docker build -f gerbil_dummy_system.docker -t git.project-hobbit.eu:4567/gerbil/gerbiltestsystem .
#	docker build -f in_memory_evaluation_storage.docker -t hobbit/in_memory_evaluation_storage .

push:
	docker push git.project-hobbit.eu:4567/gerbil/gerbilcontroller
	docker push git.project-hobbit.eu:4567/gerbil/gerbildatagenerator
	docker push git.project-hobbit.eu:4567/gerbil/gerbiltaskgenerator
	docker push git.project-hobbit.eu:4567/gerbil/gerbil-evaluation-module
	docker push git.project-hobbit.eu:4567/gerbil/gerbiltestsystem
