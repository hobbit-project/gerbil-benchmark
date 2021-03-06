default: build dockerize

build:
	cd gerbil-benchmark && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-data-generator && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-evaluation-module && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-task-generator && mvn clean package -U -Dmaven.test.skip=true
	cd gerbil-nif-system-adapter && mvn clean package -U -Dmaven.test.skip=true

dockerize:
	cd gerbil-benchmark && docker build -f gerbil_controller.docker -t git.project-hobbit.eu:4567/gerbil/gerbilcontroller .
	cd gerbil-data-generator && chmod +x init.sh && ./init.sh && docker build -f gerbil_data_generator.docker -t git.project-hobbit.eu:4567/gerbil/gerbildatagenerator .
	cd gerbil-data-generator && docker build -f bengal_data_generator.docker -t git.project-hobbit.eu:4567/conrads/bengaldatagenerator .
	cd gerbil-task-generator && docker build -f gerbil_task_generator.docker -t git.project-hobbit.eu:4567/gerbil/gerbiltaskgenerator .
	cd gerbil-task-generator && docker build -f bengal_task_generator.docker -t git.project-hobbit.eu:4567/conrads/bengaltaskgenerator .
	cd gerbil-evaluation-module && docker build -f gerbil_evaluation_module.docker -t git.project-hobbit.eu:4567/gerbil/gerbilevaluationmodule .
	cd gerbil-benchmark && docker build -f gerbil_dummy_system.docker -t git.project-hobbit.eu:4567/gerbil/gerbiltestsystem .
#	docker build -f in_memory_evaluation_storage.docker -t hobbit/in_memory_evaluation_storage .
	cd gerbil-nif-system-adapter && docker build -f gerbil_nif_system_adapter.docker -t git.project-hobbit.eu:4567/gerbil/gerbilnifsystemadapter .

indexes:
	cd gerbil-evaluation-module && chmod +x ./index.sh && ./index.sh

push:
	docker push git.project-hobbit.eu:4567/gerbil/gerbilcontroller
	docker push git.project-hobbit.eu:4567/gerbil/gerbilevaluationmodule
	docker push git.project-hobbit.eu:4567/gerbil/gerbiltestsystem
	docker push git.project-hobbit.eu:4567/gerbil/gerbilnifsystemadapter
	docker push git.project-hobbit.eu:4567/gerbil/gerbildatagenerator
	docker push git.project-hobbit.eu:4567/gerbil/gerbiltaskgenerator
	docker push git.project-hobbit.eu:4567/conrads/bengaltaskgenerator
	docker push git.project-hobbit.eu:4567/conrads/bengaldatagenerator

build-systems:
	cd gerbil-systems && mvn clean package -U -Dmaven.test.skip=true

dockerize-systems:
	cd gerbil-systems && docker build -f gerbil_systems.docker -t git.project-hobbit.eu:4567/conrads/gerbilsystems/image .

push-systems: 
	docker push git.project-hobbit.eu:4567/conrads/gerbilsystems

bengal-dg:
	cd gerbil-data-generator && mvn clean package
	cd gerbil-data-generator && docker build -f bengal_data_generator.docker -t git.project-hobbit.eu:4567/conrads/bengaldatagenerator .
	docker push git.project-hobbit.eu:4567/conrads/bengaldatagenerator
