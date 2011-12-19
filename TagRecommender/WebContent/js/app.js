Todos = SC.Application.create();

// SUGGESTION: Your tags can be stored in a similar way as how todos are stored
Todos.Todo = SC.Object.extend({
  title: null,
  isDone: false
});

Todos.Tag = SC.Object.extend({
  name: null,
  prob: 0.0
});

Todos.todosController = SC.ArrayProxy.create({
  content: [],

  createTodo: function(title) {
    var todo = Todos.Todo.create({ title: title });
    this.pushObject(todo);
  },

  clearCompletedTodos: function() {
    this.filterProperty('isDone', true).forEach(this.removeObject, this);
  },

  remaining: function() {
    return this.filterProperty('isDone', false).get('length');
  }.property('@each.isDone'),

  allAreDone: function(key, value) {
    if (value !== undefined) {
      this.setEach('isDone', value);

      return value;
    } else {
      return !!this.get('length') && this.everyProperty('isDone', true);
    }
  }.property('@each.isDone')
});

Todos.tagsController = SC.ArrayProxy.create({
  content: [{name: "#shop", prob: 0.5}],

  createTag: function(name, prob) {
    var tag = Todos.Tag.create({ name: name, prob: prob });
	this.pushObject(tag);
  },

  clearAllTags: function() {
    this.forEach(this.removeObject, this);
  }
});

Todos.StatsView = SC.View.extend({
  remainingBinding: 'Todos.todosController.remaining',

  remainingString: function() {
    var remaining = this.get('remaining');
    return remaining + (remaining === 1 ? " item" : " items");
  }.property('remaining')
});

// SUGGESTION: Think about using SC.TextArea to get a bigger input field
Todos.CreateTodoView = SC.TextField.extend({
  insertNewline: function() {
    var value = this.get('value');

    if (value) {
      Todos.todosController.createTodo(value);
      this.set('value', '');
    }
  },
  
  focusOut: function() {
	var value = this.get('value');
	
	if (value) {
		// Attempt om hetzelfde te doen als hieronder met Sproutcore functies, maar werkt ook niet voorlopig
//	  var request = SC.Request.getUrl("recommend").json();
//	  var response = request.send("{todo: " + value + "}");
//	  response.forEach(function(item){
//	    Todos.tagsController.createTag(item.name, item.prob); 
//	  });
		
	  $.get("recommend", // meer info over deze functie: http://api.jquery.com/jQuery.get/
	    {
		  todo: value
	    },
//	    Todos.tagsController.clearAllTags()  // dit werkt
	    // probleem met generische callback function met data-parameter: werkt niet, maar voorlopig weet ik niet hoe dit komt
	    function(data){
	      //data = JSON.parse(data);
	      data.forEach(function(item){
	        Todos.tagsController.createTag(item.name, item.prob);
	    	//Todos.tagsController.createTag(data, 0);
	      });
	    }
	  );
	}
  }
});

